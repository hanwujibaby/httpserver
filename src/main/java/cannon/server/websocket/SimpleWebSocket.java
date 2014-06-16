package cannon.server.websocket;

import java.nio.ByteBuffer;

public abstract class SimpleWebSocket implements WebSocket{
	
	private int maxLimit;
	
	private byte[] buf;
	private int index;
	public SimpleWebSocket(int maxLimit){
		this.maxLimit = maxLimit;
	}
	
	
	@Override
	public void onInit(boolean endFragment, long payloadLength)throws WebSocketException {
		if(payloadLength>maxLimit){
			throw new WebSocketException("fragment data length is too big : "+payloadLength);
		}
		buf = new byte[(int)payloadLength];
		index = 0;
	}

	@Override
	public void onRead(ByteBuffer buffer, int length)throws WebSocketException {
		buffer.get(buf, index, length);
		index+=length;
	}

	@Override
	public void onComplete()throws WebSocketException {
		onMessage(buf);
		buf = null;
		index = 0;
	}
	
	public abstract void onMessage(byte[] message)throws WebSocketException;
}
