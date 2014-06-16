package cannon.server.controller;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Controller;

import cannon.server.http.DefaultHttpRequest;
import cannon.server.http.HttpException;
import cannon.server.http.HttpHeaders;
import cannon.server.http.HttpMessageHandler;
import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;
import cannon.server.http.HttpResponseStatus;
import cannon.server.util.ServerConfig;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-10
 * @qq 271398203
 * @todo 用过Spring MVC吗？ 这个类就是为了打造一个类似MVC Controller的一个CGI反射方案
 */
public final class ControllerMessageHandler extends HttpMessageHandler{
	private static Logger logger = LoggerFactory.getLogger(ControllerMessageHandler.class);
	private static Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}|\\*");
	private ClassPathXmlApplicationContext context;
	private Map<String,MethodHandler> mappingMethods = new HashMap<String,MethodHandler>();
	private Map<String,Template> templates = new HashMap<String,Template>();
	private List<MethodHandler> matcherMethods =  new ArrayList<MethodHandler>();
	private ObjectMapper defaultObjectMapper;
	private ObjectMapper xssFilterObjectMapper;
	private Charset charset;
	private String jsonContentType;

	private GlobalInterceptor globalInterceptor;
	private ExceptionHandler exceptionHandler;
	@Override
	public void initialize(ServerConfig serverConfig) {
		String charset = serverConfig.getString("server.http.charset", "UTF-8");
		this.charset = Charset.forName(charset);
		this.jsonContentType = "text/json; charset="+charset;
		
		defaultObjectMapper = new ObjectMapper();
		xssFilterObjectMapper = new ObjectMapper();
		CustomSerializerFactory serializerFactory= new CustomSerializerFactory();
		serializerFactory.addSpecificMapping(String.class, new XssFilterJsonSerializer());
		xssFilterObjectMapper.setSerializerFactory(serializerFactory);
		
		context = new ClassPathXmlApplicationContext(new String[]{"classpath:application.xml"});
		
		try{
			this.globalInterceptor = context.getBean(GlobalInterceptor.class);
		}catch(Exception e){
			//ignore null
		}
		try{
			this.exceptionHandler = context.getBean(ExceptionHandler.class);
		}catch(Exception e){
			
		}
		
		Map<String, Object> beans = context.getBeansWithAnnotation(Controller.class);
		logger.info("beans:{}",beans.size());
		for(Entry<String,Object> entry:beans.entrySet()){
			loadObject(entry.getValue());
		}
		
	}
	
	@Override
	public void destroy() {
		context.destroy();
	}



	private void loadObject(Object object){
		Class<?> clazz = object.getClass();
		Method[] methods = clazz.getDeclaredMethods();
		for(Method method:methods){
			if(method.isAnnotationPresent(RequestMapping.class)){
				RequestMapping mapping = method.getAnnotation(RequestMapping.class);
				String[] values = mapping.value();
				for(String value:values){
					loadMethod(object,clazz,method,value);
				}
			}
		}
	}
	private void loadMethod(Object object,Class<?> clazz ,Method method,String mapping){
		method.setAccessible(true);
		MethodHandler methodHandler = new MethodHandler(object,method);
		
		Matcher matcher = PATH_VARIABLE_PATTERN.matcher(mapping);
		
		String matched = null;
		List<String> pathVariables = new ArrayList<String>();
		while(matcher.find()){
			String quot = matcher.group(0);
			if("*".equals(quot)){
				matched = matcher.replaceFirst(".+");
			}else{
				String group = matcher.group(1);
				pathVariables.add(group);
				matched = matcher.replaceFirst("(\\\\w+)");
			}
			matcher = PATH_VARIABLE_PATTERN.matcher(matched);
		}
		if(matched!=null){
			matched = "^"+matched+"$";
			methodHandler.setPathPattern(Pattern.compile(matched),pathVariables.toArray(new String[0]));
		}
		methodHandler.setObjectMapper(defaultObjectMapper);
		methodHandler.setParameterTypes(clazz,method);
		
		if(methodHandler.isMatcherHandler()){
			matcherMethods.add(methodHandler);
		}else{
			mappingMethods.put(mapping, methodHandler);
		}
		
		if(methodHandler.isVelocityTemplate()){
			String name = methodHandler.getTemplateName();
			Template template = Velocity.getTemplate(methodHandler.getTemplateName());
			templates.put(name, template);
		}
		
		logger.info("mapped method({}.{}) "+mapping,clazz.getSimpleName(),method.getName());
	}


	@Override
	public byte[] service(HttpRequest request, HttpResponse response)throws Throwable {
		try{
			String queryString = request.getQueryString();
			MethodHandler handler = mappingMethods.get(queryString);
			if(handler==null){
				for(MethodHandler mm:matcherMethods){
					Matcher m = mm.getPattern().matcher(queryString);
					if(m.find()){
						int index = 1;
						DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest)request;
						for(String key:mm.getKeys()){
							String value = m.group(index);
							defaultHttpRequest.addParameter(key, value);
							index++;
						}
						handler = mm;
						break;
					}
				}
			}
			Object o = handler!=null?handler.getObject():null;
			
			if(globalInterceptor!=null&&!globalInterceptor.beforeHandle(request, response,o)){
				return response.getContent();
			}
			
			if(handler==null){
				throw new HttpException(HttpResponseStatus.NOT_FOUND);
			}
			
			Object result = handler.invoke(request, response);
			byte[] bytes = null;
			if(handler.isVelocityTemplate()){
				Map<?,?> params =  null;
				if(result instanceof Map){
					params = (Map<?,?>)result;
				} 
				VelocityContext context = new VelocityContext(params);
				
				Template template = templates.get(handler.getTemplateName());
				if(template == null){
					throw new RuntimeException("Can not find the velocity template:"+handler.getTemplateName());
				}
				ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
				OutputStream stream = os;
				String acceptEncoding = request.getHeader("Accept-Encoding");
				String[] arr = StringUtils.split(acceptEncoding,',');
				
				String encoding = null;
				
				//如果浏览器支持deflate则优先采用此算法压缩
				int type = 0;
				for(String a:arr){
					if("gzip".equals(a)&&encoding==null){
						type=1;
					}else if("deflate".equals(a)){
						type=2;
						break;
					}
				}
				
				if(type==2){
					stream = new DeflaterOutputStream(stream);
					response.addHeader(HttpHeaders.Names.CONTENT_ENCODING, "deflate");
				}else if(type==1){
					stream = new GZIPOutputStream(stream);
					response.addHeader(HttpHeaders.Names.CONTENT_ENCODING, "gzip");
				}
				
				Writer writer = new PrintWriter(stream);
				
				template.merge(context, writer);
				writer.flush();
				writer.close();
				bytes = os.toByteArray();
				stream.close();
			}else if(result!=null){
				if(handler.isResponseBody()){
					response.setHeader(HttpHeaders.Names.CONTENT_TYPE, this.jsonContentType);
					if(handler.isXssFilter()){
						bytes = xssFilterObjectMapper.writeValueAsBytes(result);
					}else{
						bytes = defaultObjectMapper.writeValueAsBytes(result);
					}
				}else{
					if(result instanceof byte[]){
						bytes = (byte[])result;
					}else{
						String str = result.toString();
						if(handler.isXssFilter()){
							bytes = StringEscapeUtils.escapeHtml4(str).getBytes(charset);
						}else{
							bytes = str.getBytes(charset);
						}
					}
				}
			}
			if(globalInterceptor!=null){
				globalInterceptor.afterHandle(request, response, handler);
			}
			
			return bytes;
		}catch(Throwable t){
			if(this.exceptionHandler!=null){
				return this.exceptionHandler.resolveException(request, response, t);
			}else{
				throw t;
			}
		}
	}

}
