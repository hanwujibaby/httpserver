package cannon.server.http;

import cannon.server.util.ServerConfig;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 服务处理接口，类似HttpServlet抽象类，处理请求的时候只需要实现这个接口即可
 */
public abstract class HttpMessageHandler {
	public void initialize(ServerConfig serverConfig){
		
	}
	public void destroy(){
		
	}
	public abstract byte[] service(HttpRequest request, HttpResponse response) throws Throwable;
}
