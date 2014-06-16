package test.web.controller;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.websocket.SimpleWebSocket;
import cannon.server.websocket.WebSocketCallback;
import cannon.server.websocket.WebSocketSession;


public class ChartSession extends SimpleWebSocket implements WebSocketSession{
	private Map<String,ChartSession> sessions;
	private String name;
	public ChartSession(String name ,Map<String,ChartSession> sessions) {
		super(65535);
		this.sessions = sessions;
		this.name = name;
	}
	private static final Logger logger = LoggerFactory.getLogger(ChartSession.class);
	private WebSocketSession session;
	@Override
	public void onClose() {
		logger.info("{} exit",name);
		sessions.remove(name);
	}
	@Override
	public void setWebSocketSession(WebSocketSession session) {
		this.session = session;
	}
	@Override
	public void onMessage(byte[] message) {
		String messageStr;
		try {
			messageStr = new String(message,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			messageStr = new String(message);
		}
		logger.info("{} say : ",messageStr);
		
		Iterator<ChartSession> iter = sessions.values().iterator();
		byte[] m;
		try {
			m = (name+" say : "+messageStr).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			m = (name+" say : "+messageStr).getBytes();
		}
		while(iter.hasNext()){
			ChartSession s = iter.next();
			try {
				s.sendText(m, 1, TimeUnit.SECONDS, null);
			} catch (InterruptedException e) {}
		}
		
	}
	@Override
	public Future<Void> sendText(byte[] bytes, long timeout, TimeUnit unit,
			WebSocketCallback callback) throws InterruptedException {
		return session.sendText(bytes, timeout, unit, callback);
	}
	@Override
	public Future<Void> sendBinary(byte[] bytes, long timeout, TimeUnit unit,
			WebSocketCallback callback) throws InterruptedException {
		// TODO Auto-generated method stub
		return session.sendBinary(bytes, timeout, unit, callback);
	}
	@Override
	public Future<Void> close(WebSocketCallback callback, long timeout,
			TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return session.close(callback, timeout, unit);
	}
}
