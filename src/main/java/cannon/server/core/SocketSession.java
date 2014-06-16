package cannon.server.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.http.HttpProcessor;
import cannon.server.websocket.WebSocket;
import cannon.server.websocket.WebSocketProcessor;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 套接字会话，用于记录客户端连接，并且缓存会话所需要使用的一些数据
 */
public final class SocketSession{
	private final Logger logger = LoggerFactory.getLogger(SocketSession.class);

	private ByteBuffer buffer;
	private AsynchronousSocketChannel socket;
	private long timeout;

	private HttpServer server;
	private SocketReadHandler socketReadHandler;
	private ProtocolProcessor processor;

	private boolean isClosed = false;

	private InetAddress remoteAddress;
	
	public SocketSession(AsynchronousSocketChannel socket,HttpServer server) throws Exception{
		this.socket = socket;
		this.timeout = server.getTimeout();
		this.server = server;
		this.socketReadHandler = server.getSocketReadHandler();
		
		this.remoteAddress = ((InetSocketAddress)socket.getRemoteAddress()).getAddress();
		/*
		 * 构建Socket Session 的时候借一个read buffer，当SocketSession Close的时候还 
		 */
		this.buffer = server.borrowObject();
		this.processor = new HttpProcessor(this,server);
	}
	
	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void process() {
		try {
			processor.process();
		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
			this.close();
		}
	}
	

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void read(){
		this.buffer.clear();
		this.socket.read(this.buffer,timeout,TimeUnit.MILLISECONDS,this,socketReadHandler);
	}

	public <A> void write(ByteBuffer buffer,A attachment,CompletionHandler<Integer, ? super A> handler){
		this.socket.write(buffer, timeout, TimeUnit.MILLISECONDS, attachment, handler);
	}
	public Future<Integer> write(ByteBuffer buffer){
		return this.socket.write(buffer);
	}
	public void close(){
		/**
		 * 这个地方上锁是防止重复关闭
		 * 当WebSocket 关闭帧写出后，服务端无论客户端是否有返回都会主动进入关闭流程
		 * 但是如果客户端收到关闭帧之后又成功以关闭帧的形式反馈给了服务器，服务器处于全双工的读状态，
		 * 而且aio channel线程的优先级比较高，所以读写线程都会进入Socket Session关闭流程
		 * 这样就发生了两次关闭操作，在这里将对象上锁并且判断是否关闭用来过滤掉多余的关闭操作
		 */
		synchronized (this) {
			if(isClosed){
				return ;
			}else{
				isClosed=true;
			}
		}
		
		if(buffer!=null){
			buffer.clear();
			try {
				server.returnObject(buffer);
			} catch (Throwable e) {
				//ignore
			}
		}
		if(this.socket!=null){
			try {
				this.socket.close();
			} catch (Throwable e) {
				//ignore
			}
		}
//		logger.debug("Socket Session({}) Closed",this.hashCode());
		if(this.processor!=null){
			try{
				processor.close();
			}catch(Throwable e){
				logger.error(e.getMessage(),e);
			}
		}
		
		//Release All Reference
		socket=null;
		buffer=null;
		server=null;
		processor=null;
	}

	public void upgrade(WebSocket webSocket) {
		this.timeout = 0;
		this.processor = new WebSocketProcessor(this,server,webSocket);
	}

}
