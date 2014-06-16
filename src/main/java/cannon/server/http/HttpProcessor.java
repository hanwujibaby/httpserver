package cannon.server.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.core.HttpServer;
import cannon.server.core.ProtocolProcessor;
import cannon.server.core.SocketSession;
import cannon.server.http.HttpHeaders.Names;
import cannon.server.http.HttpHeaders.Values;
import cannon.server.websocket.WebSocket;

public final class HttpProcessor implements ProtocolProcessor,Runnable,CompletionHandler<Integer, ByteBuffer>{
	private final Logger logger = LoggerFactory.getLogger(HttpProcessor.class);

	private final SocketSession session;
	private final HttpMessageHandler dynamicHandler;
	private final StaticHandler staticHandler;
	private final HttpMessageSerializer serializer;
	private final HttpServer server;
	private final SimpleDateFormat dateFormat;
	private final String htmlContentType;
	private final int cacheControlMaxAge;
	private DefaultHttpRequest request;
	private boolean keepAlive=false;

	private HttpSocketStatus socketStatus;
	//这个buffer 只直接使用堆中byte数组包装出来的ByteBuffer
	private ByteBuffer outputContent;
	private WebSocket webSocket;
	
	public HttpProcessor(SocketSession session, HttpServer server){
		this.session = session;
		this.dynamicHandler = server.getDynamicHandler();
		this.staticHandler = server.getStaticHandler();
		this.serializer =  server.getHttpMessageSerializer();
		this.server = server;
		this.dateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.socketStatus = HttpSocketStatus.SKIP_CONTROL_CHARS;
		
		this.htmlContentType = server.getHtmlContentType();
		this.cacheControlMaxAge = server.getCacheControlMaxAge();
	}

	public HttpSocketStatus getSocketStatus() {
		return socketStatus;
	}

	public void setSocketStatus(HttpSocketStatus socketStatus) {
		this.socketStatus = socketStatus;
	}

	public DefaultHttpRequest getRequest() {
		return request;
	}

	public void setRequest(DefaultHttpRequest request) {
		this.request = request;
	}

	@Override
	public void process()throws Throwable {
		ByteBuffer buffer = session.getBuffer();
		try {
			if(serializer.decode(buffer, this)){
				buffer.clear();
				server.execute(this);
			}else{
				session.read();
			}
		} catch (Throwable t) {
			buffer.clear();
			this.keepAlive=false;
			HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			if(t instanceof HttpException){
				//已知异常无需打印日志
				HttpException e = (HttpException)t;
				status = e.getStatus();
				logger.warn(e.getMessage());
			}else{
				//未知异常打印日志
				logger.error(t.getMessage(),t);
			}
			DefaultHttpResponse response = null;
			if(this.request==null){
				response = createResponse(HttpVersion.HTTP_1_1, status);
			}else{
				response = createResponse(request.getProtocolVersion(), status);
			}
			response.setHeader(Names.CONNECTION, Values.CLOSE);
			response.setStatus(status);
			byte[] content = status.getBytes();
			write(response,content);
		}
	}

	@Override
	public void run() {
		try{
			this.execute();
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			/*
			 * 这里出了问题肯定是代码问题  ^_^!
			 */
			session.close();
		}
	}

	private void execute() throws Exception{
		DefaultHttpRequest request = this.request;
		this.request=null;
		DefaultHttpResponse response = createResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
		byte[] content = null;
		try{
			keepAlive = HttpHeaders.isKeepAlive(request);
			if(keepAlive){
				response.addHeader(Names.CONNECTION, Values.KEEP_ALIVE);
			}else{
				response.addHeader(Names.CONNECTION, Values.CLOSE);
			}
			if(request.isDynamic()){
				try{
					/**
					 * 如果中间层有HTTP代理需要从client-ip或则x-forwarded-for中取客户端IP
					 */
					String clientIp = request.getHeader("Client-IP");
					if(clientIp!=null){
						request.setClientIp(clientIp);
					}else{
						clientIp = request.getHeader("X-Forwarded-For");
						if(clientIp!=null){
							request.setClientIp(clientIp);
						}else{
							InetAddress remoteAddress = session.getRemoteAddress();
							if(remoteAddress.isLinkLocalAddress()){
								request.setClientIp("127.0.0.1");
							}else{
								request.setClientIp(remoteAddress.getHostAddress());
							}
						}
					}
					/**
					 * 设置默认输出Content-Type为text/html; charset=UTF-8
					 * 告诉客户端禁止启用缓存
					 */
					response.addHeader(Names.CONTENT_TYPE, htmlContentType);
					if(HttpVersion.HTTP_1_0==response.getProtocolVersion()){
						response.addHeader(HttpHeaders.Names.PRAGMA, HttpHeaders.Values.NO_CACHE);
					}else{
						response.addHeader(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_STORE);
					}
					
					request.initialize();
					content = this.dynamicHandler.service(request, response);
					
					WebSocket webSocket = response.getWebSocket();
					if(webSocket!=null){
						this.webSocket = webSocket;
					}
				}catch(Throwable t){
					HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
					if(t instanceof HttpException){
						status = ((HttpException)t).getStatus();
					}else{
						logger.error(t.getMessage(),t);
					}
					byte[] statusPage = status.getBytes();
					response.setStatus(status);
					response.setHeader(Names.CONNECTION, Values.CLOSE);
					keepAlive = false;
					content = statusPage;
				}
			}else if(staticHandler!=null){
				String queryString = request.getQueryString();
				StaticFile file = staticHandler.find(queryString);
				if(file==null){
					response.setStatus(HttpResponseStatus.NOT_FOUND);
					keepAlive = false;
					response.setHeader(Names.CONNECTION, Values.CLOSE);
					response.addHeader(Names.CONTENT_TYPE, htmlContentType);
					content = HttpResponseStatus.NOT_FOUND.getBytes();
				}else{
					String clientModifiedSince = request.getHeader("If-Modified-Since");
					String serverModifiedSince = file.getLastModified();
					response.addHeader(HttpHeaders.Names.LAST_MODIFIED, serverModifiedSince);
					response.addHeader(Names.CONTENT_TYPE, file.getContentType());

					if(serverModifiedSince.equals(clientModifiedSince)){
						/*
						 * 304告诉浏览器，服务器文件没有发生修改
						 */
						response.setStatus(HttpResponseStatus.NOT_MODIFIED);
					}else{
						//返回静态文件
						/*
						 * HTTP1.0+ 都支持绝对时间过期头Expire
						 * HTTP1.1+ 都支持相对时间过期头Cache-Control:max-age=3600,单位秒
						 */
						if(cacheControlMaxAge>0){
							if(response.getProtocolVersion()==HttpVersion.HTTP_1_1){
								response.addHeader(HttpHeaders.Names.CACHE_CONTROL, "max-age="+cacheControlMaxAge);
							}else{
								Date expire = new Date(cacheControlMaxAge*1000+System.currentTimeMillis());
								response.addHeader(HttpHeaders.Names.EXPIRES, dateFormat.format(expire));
							}
						}
						content = file.getBytes();
					}
				}
			}else{
				response.setStatus(HttpResponseStatus.NOT_FOUND);
				keepAlive = false;
				response.setHeader(Names.CONNECTION, Values.CLOSE);
				response.addHeader(Names.CONTENT_TYPE, htmlContentType);
				content = HttpResponseStatus.NOT_FOUND.getBytes();
			}
		}finally{
			request.destroy();
			write(response,content);
		}
	}
	private void write(DefaultHttpResponse response, byte[] content) throws Exception {
		this.socketStatus = HttpSocketStatus.WRITING;
		int contentLength = (content!=null&&content.length>0)?content.length:0;
		response.setHeader(Names.CONTENT_LENGTH, contentLength);
		if(contentLength>0){
			this.outputContent = ByteBuffer.wrap(content);
		}
		this.socketStatus = HttpSocketStatus.WRITING;

		ByteBuffer outputHeader = session.getBuffer();
		serializer.encodeInitialLine(outputHeader, response);
		serializer.encodeHeaders(outputHeader, response,this.session);
		outputHeader.flip();
		session.write(outputHeader, outputHeader, this);
	}

	public DefaultHttpResponse createResponse(HttpVersion version,HttpResponseStatus status){
		DefaultHttpResponse response = new DefaultHttpResponse(version, HttpResponseStatus.OK);
		response.addHeader(Names.SERVER, server.getName());
		response.addHeader(Names.DATE, dateFormat.format(new Date()));
		return response;
	}
	@Override
	public void completed(Integer result, ByteBuffer outputHeader) {
		if(outputHeader!=null&&outputHeader.remaining()>0){
			session.write(outputHeader, outputHeader, this);
		}
		if(this.outputContent!=null){
			if(this.outputContent.remaining()>0){
				session.write(this.outputContent, null, this);
				return;
			}else{
				this.outputContent=null;
			}
		}
		if(!this.keepAlive){
			session.close();
			return;
		}
		this.socketStatus = HttpSocketStatus.SKIP_CONTROL_CHARS;
		if(webSocket!=null){
			session.upgrade(webSocket);
		}
		session.read();
	}

	@Override
	public void failed(Throwable exc, ByteBuffer outputHeader) {
		session.close();
	}

	@Override
	public void close() {
		//ignore
	}

}
