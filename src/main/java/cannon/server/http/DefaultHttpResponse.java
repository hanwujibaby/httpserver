package cannon.server.http;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import cannon.server.websocket.WebSocket;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo HttpResponse 实现类，用于保存HTTP请求栈上数据
 */
public class DefaultHttpResponse extends DefaultHttpMessage implements HttpResponse{
	private HttpResponseStatus status;
	private WebSocket webSocket;
	private byte[] content;
	
	public DefaultHttpResponse(HttpVersion version,HttpResponseStatus status) {
		 super(version);
	     setStatus(status);
	}
	@Override
	public HttpResponseStatus getStatus() {
		return status;
	}
	@Override
	public void setStatus(HttpResponseStatus status) {
		if (status == null) {
            throw new NullPointerException("status");
        }
        this.status = status;
	}
	@Override
	public boolean upgrade(HttpRequest request,WebSocket webSocket) {
		String key = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_KEY);
		String upgrade = request.getHeader(HttpHeaders.Names.UPGRADE);
		String connection = request.getHeader(HttpHeaders.Names.CONNECTION);
		int version = HttpHeaders.getIntHeader(request, HttpHeaders.Names.SEC_WEBSOCKET_VERSION,-1);
		
		if(HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(upgrade)&&
				HttpHeaders.Values.UPGRADE.equalsIgnoreCase(connection)&&version==13&&key!=null){
			setStatus(HttpResponseStatus.SWITCHING_PROTOCOLS);
			setHeader(HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET);
			setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.UPGRADE);
			
			setHeader(HttpHeaders.Names.SEC_WEBSOCKET_ACCEPT, Base64.encodeBase64String(DigestUtils.sha1(key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11")));
			this.webSocket = webSocket;
			return true;
		}else{
			return false;
		}
	}
	public WebSocket getWebSocket(){
		return this.webSocket;
	}
	@Override
	public void sendRedirect(String url) {
		this.status = HttpResponseStatus.FOUND;
		addHeader(HttpHeaders.Names.LOCATION, url);
	}
	@Override
	public void setContent(byte[] content) {
		this.content = content;
	}
	@Override
	public byte[] getContent() {
		return this.content;
	}
}
