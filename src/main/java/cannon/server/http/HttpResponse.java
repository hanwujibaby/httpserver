package cannon.server.http;

import cannon.server.websocket.WebSocket;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo Response 对外接口类，实现业务的时候只需操作这个接口即可
 */
public interface HttpResponse extends HttpMessage {
    HttpResponseStatus getStatus();
    void setStatus(HttpResponseStatus status);
	void sendRedirect(String url);
	void setContent(byte[] content);
	byte[] getContent();
	/**
	 * @return 将HTTP通道升级为全双工通信通道，返回true为升级成功，false为升级失败，可能是非法请求必要缺少Header
	 */
	boolean upgrade(HttpRequest request,WebSocket webSocket);
	
}