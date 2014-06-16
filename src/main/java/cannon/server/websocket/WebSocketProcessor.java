package cannon.server.websocket;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.core.HttpServer;
import cannon.server.core.ProtocolProcessor;
import cannon.server.core.SocketSession;

public final class WebSocketProcessor implements ProtocolProcessor,Runnable,CompletionHandler<Integer, Object>{
	private final static Logger logger= LoggerFactory.getLogger(WebSocketProcessor.class);
	private final SocketSession session;
	private final HttpServer server;
	private final WebSocket webSocket;
	private final WebSocketSerializer webSocketSerializer;

	private WebSocketStatus status;
	private boolean keepAlive = true;

	/*
	 * Read Data
	 */
	private int opcode;
	private long payloadLength;
	private byte[] mask;
	private boolean endFragment;
	private long payloadIndex;
	
	/*
	 * Write Data
	 * outputHead 和 outputBody 都是直接由byte[] 数组包装出来的.
	 */
	private ByteBuffer outputHead;
	private ByteBuffer outputBody;
	private WebSocketFuture sendFuture;
	private ReentrantLock lock = new ReentrantLock();
	private Condition notNull = lock.newCondition();

	public WebSocketProcessor(SocketSession session ,HttpServer server,WebSocket webSocket){
		this.session = session;
		this.server = server;
		this.webSocket = webSocket;
		this.status = WebSocketStatus.READ_HEAD;
		this.webSocketSerializer = server.getWebSocketSerializer();
		webSocket.setWebSocketSession(new WebSocketAdapter());
	}
	
	
	


	@Override
	public void process() {
		try {
			if(webSocketSerializer.decode(session.getBuffer(), this)){
				if(this.endFragment){
					server.execute(this);
				}else{
					session.read();
				}
			}else{
				session.read();
			}
		} catch (WebSocketException e) {
			logger.info(e.getMessage());
			this.close();
		}
	}
	
	@Override
	public void run() {
		try{
			execute();
		}catch(Exception e){
			if(e instanceof WebSocketException){
				logger.warn(e.getMessage());
			}else{
				logger.error(e.getMessage(),e);
			}
			this.session.close();
		}
	}
	private void execute() throws WebSocketException{
		if(this.opcode==1||this.opcode==2){
			//文本帧 或 二进制帧
			this.webSocket.onComplete();
			read();
		}else if(opcode==8){
			//关闭帧
			this.session.close();
		}else{
			read();
		}
	}
	private void read(){
		this.status = WebSocketStatus.READ_HEAD;
		this.session.read();
	}

	
	private class WebSocketAdapter implements WebSocketSession{
		private static final byte TEXT_OPCODE   = (byte)0B10000001;
		private static final byte BINARY_OPCODE = (byte)0B10000010;
		private static final byte CLOSE_OPCODE = (byte)0B10001000;
		@Override
		public Future<Void> sendText(byte[] bytes,long timeout,TimeUnit unit,WebSocketCallback callback) throws InterruptedException {
			return send(bytes,TEXT_OPCODE,timeout,unit,callback);
		}

		@Override
		public Future<Void> sendBinary(byte[] bytes,long timeout,TimeUnit unit,WebSocketCallback callback) throws InterruptedException {
			return send(bytes,BINARY_OPCODE,timeout,unit,callback);
		}

		@Override
		public Future<Void> close(WebSocketCallback callback,long timeout,TimeUnit unit) throws InterruptedException {
			return send(null,CLOSE_OPCODE,timeout,unit,callback);
		}
		private Future<Void> send(byte[] bytes,byte opcode,long timeout,TimeUnit unit,WebSocketCallback callback) throws InterruptedException{
			lock.lock();
			try {
				if(sendFuture!=null){
					notNull.await(timeout, unit);
				}
			}finally{
				lock.unlock();
			}
			int payloadLength = 0;
			if(bytes!=null&&bytes.length>0){
				outputBody = ByteBuffer.wrap(bytes);
				payloadLength = bytes.length;
			}
			byte[] headBytes=null;
			if(payloadLength>65535){
				//扩展字段使用8字节
				headBytes = new byte[10];
				headBytes[0] = opcode;
				headBytes[1] = 127;
				headBytes[2] = (byte)((payloadLength>>56) & 0xFF);
				headBytes[3] = (byte)((payloadLength>>48) & 0xFF);
				headBytes[4] = (byte)((payloadLength>>40) & 0xFF);
				headBytes[5] = (byte)((payloadLength>>32) & 0xFF);
				headBytes[6] = (byte)((payloadLength>>24) & 0xFF);
				headBytes[7] = (byte)((payloadLength>>16) & 0xFF);
				headBytes[8] = (byte)((payloadLength>>8) & 0xFF);
				headBytes[9] = (byte)(payloadLength & 0xFF);
			}else if(payloadLength>125){
				//扩展字段使用2字节
				headBytes = new byte[4];
				headBytes[0] = opcode;
				headBytes[1] = 126;
				headBytes[2] = (byte)((payloadLength>>8) & 0xFF);
				headBytes[3] = (byte)(payloadLength & 0xFF);
			}else{
				//无扩展字段
				headBytes = new byte[2];
				headBytes[0] = opcode;
				headBytes[1] = (byte)payloadLength;
			}
			//因为服务器发送的数据都不能掩码所以无需创建后面四个字节
			if(opcode==CLOSE_OPCODE){
				keepAlive=false;
			}
			outputHead = ByteBuffer.wrap(headBytes);
			session.write(outputHead, null, WebSocketProcessor.this);

			sendFuture = new WebSocketFuture(callback);
			return sendFuture;
		}
	}


	@Override
	public void completed(Integer result, Object attachment) {
		if(outputHead!=null){
			int remain = outputHead.remaining();
			if(remain>0){
				session.write(outputHead, null, WebSocketProcessor.this);
				return;
			}else{
				outputHead = null;
			}
		}
		if(outputBody!=null){
			int remain = outputBody.remaining();
			if(remain>0){
				session.write(outputBody, null, WebSocketProcessor.this);
				return ;
			}else{
				outputBody = null;
			}
		}

		if(!keepAlive){
			session.close();
		}

		if(sendFuture!=null){
			sendFuture.done();
			sendFuture = null;
		}
		lock.lock();
		try {
			notNull.signal();
		}finally{
			lock.unlock();
		}
	}

	@Override
	public void failed(Throwable t, Object attachment) {
		session.close();
		if(sendFuture!=null){
			sendFuture.exception(t);
			sendFuture = null;
		}
		lock.lock();
		try {
			notNull.signal();
		}finally{
			lock.unlock();
		}
	}

	@Override
	public void close() {
		try {
			this.webSocket.onClose();
		} catch (WebSocketException e) {
			logger.warn(e.getMessage());
		}
	}

	WebSocketStatus getStatus() {
		return status;
	}
	void setStatus(WebSocketStatus status) {
		this.status = status;
	}
	void init(boolean endFragment, int opcode, byte[] mask,
			long payloadLength) throws WebSocketException {
		this.endFragment = endFragment;
		this.mask = mask;
		this.payloadLength = payloadLength;
		this.payloadIndex = 0L;
		if(opcode!=0){
			this.opcode = opcode;
			this.webSocket.onInit(endFragment,payloadLength);
		}
	}
	boolean readPayloadData(ByteBuffer buffer) throws WebSocketException{
		int remain = buffer.remaining();
		int index = buffer.position();
		
		long need = this.payloadLength-this.payloadIndex;
		
		int length;
		int end;
		if(need>=remain){
			//全部读取
			length = remain;
			end = index+remain;
		}else{
			//部分读取
			length = (int)(remain-need);
			end = index+length;
		}
		for(;index<end;index++,this.payloadIndex++){
			byte masked = buffer.get(index);
			masked = (byte)(masked ^ (mask[(int)(this.payloadIndex%4)] & 0xFF));
			buffer.put(index, masked);
		}
		webSocket.onRead(buffer, length);
		buffer.position(index);
		if(this.payloadIndex>=this.payloadLength){
			this.status = WebSocketStatus.RUNNING;
			return true;
		}else{
			return false;
		}
	}

}
