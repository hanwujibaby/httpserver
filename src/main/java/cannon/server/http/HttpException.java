package cannon.server.http;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo Http 请求异常，并按已知返回码返回
 */
public class HttpException extends Exception{
	private static final long serialVersionUID = 1L;
	private final HttpResponseStatus status;
	private final String message;
	public HttpException(HttpResponseStatus status,String message){
		this.status = status;
		this.message = message;
	}
	public HttpException(HttpResponseStatus status){
		this(status,null);
	}
	
	public HttpException(){
		this(HttpResponseStatus.INTERNAL_SERVER_ERROR,null);
	}
	public HttpResponseStatus getStatus() {
		return status;
	}
	public String getMessage() {
		return message;
	}
	
}
