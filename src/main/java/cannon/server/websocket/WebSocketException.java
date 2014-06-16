package cannon.server.websocket;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-12
 * @qq 271398203
 * @todo 数据帧服务器无法处理的异常
 */
public class WebSocketException extends Exception{

	private static final long serialVersionUID = 1L;

	public WebSocketException(String message) {
		super(message);
	}
}
