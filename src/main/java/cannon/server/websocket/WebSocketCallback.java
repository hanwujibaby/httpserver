package cannon.server.websocket;

public interface WebSocketCallback {
	void success();
	void error(Throwable t);
}
