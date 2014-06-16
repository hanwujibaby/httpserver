package cannon.server.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.controller.RequestParamType.Type;
import cannon.server.http.HttpException;
import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;
import cannon.server.http.HttpResponseStatus;

public final class MethodHandler {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandler.class);
	private final Object object;
	private final Method method;
	private final boolean isResponseBody;
	private final boolean xssFilter;
	
	
	private RequestParamType[] requestParamTypes;
	private int parameterLength;
	private ObjectMapper objectMapper;
	private boolean matcherHandler = false;
	private Pattern pattern;
	private String[] keys;
	private boolean isVelocityTemplate;
	private String templateName;
	
	public MethodHandler(Object object,Method method){
		this.object = object;
		this.method = method;
		
		this.isVelocityTemplate = method.isAnnotationPresent(VelocityTemplate.class);
		this.isResponseBody=method.isAnnotationPresent(ResponseBody.class);
		if(this.isVelocityTemplate&&this.isResponseBody){
			throw new RuntimeException(object.getClass().getName()+" method "+method.getName()+" can not be annotation present both VelocityTemplate and ResponseBody");
		}
		if(this.isVelocityTemplate){
			this.templateName = method.getAnnotation(VelocityTemplate.class).value();
		}
		XssFilter filter = method.getAnnotation(XssFilter.class);
		this.xssFilter = filter==null?true:filter.value();
	}
	
	public String getTemplateName() {
		return templateName;
	}
	
	public boolean isVelocityTemplate() {
		return isVelocityTemplate;
	}

	public Pattern getPattern() {
		return pattern;
	}
	
	public String[] getKeys() {
		return keys;
	}

	public boolean isMatcherHandler() {
		return matcherHandler;
	}
	void setPathPattern(Pattern pattern,String[] keys) {
		this.pattern = pattern;
		this.matcherHandler = true;
		this.keys = keys;
	}
	
	void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	public Method getMethod() {
		return method;
	}
	
	public Object getObject() {
		return object;
	}

	void setParameterTypes(Class<?> clazz,Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		this.parameterLength = parameterTypes.length;
		this.requestParamTypes = new RequestParamType[this.parameterLength];
		
		for(int i=0;i<this.parameterLength;i++){
			Class<?> classType = parameterTypes[i];
			RequestParamType paramType = new RequestParamType();
			Type type;
			if(classType==HttpRequest.class){
				type = Type.HTTP_REQUEST;
			}else if(classType==HttpResponse.class){
				type = Type.HTTP_RESPONSE;
			}else{
				if(classType==String.class){
					type = Type.STRING;
				}else if(classType.isAssignableFrom(List.class)){
					type = Type.LIST;
				}else if(classType.isAssignableFrom(Set.class)){
					type = Type.SET;
				}else if(classType.isAssignableFrom(Map.class)){
					type = Type.MAP;
				}else if(classType.isArray()){
					type = Type.ARRARY;
				}else if(classType==Boolean.class||classType==boolean.class){
					type = Type.BOOLEAN;
				}else if(classType==Short.class||classType==short.class){
					type = Type.SHORT;
				}else if(classType==Integer.class||classType==int.class){
					type = Type.INTEGER;
				}else if(classType==Long.class||classType==long.class){
					type = Type.LONG;
				}else if(classType==Float.class||classType==float.class){
					type = Type.FLOAT;
				}else if(classType==Double.class||classType==double.class){
					type = Type.DOUBLE;
				}else if(classType==Character.class||classType==char.class){
					type = Type.CHAR;
				}else if(classType==Byte.class||classType==byte.class){
					type = Type.BYTE;
				}else{
					throw new RuntimeException(clazz.getSimpleName()+"."+method.getName()+" param["+i+"] is not suported to request params ioc");
				}
				
				
				Annotation[] annotations = parameterAnnotations[i];
				RequestParam requestParam = null;
				PathVariable pathVariable = null;
				for(Annotation annotation:annotations){
					if(annotation instanceof RequestParam){
						requestParam = (RequestParam)annotation;
						paramType.setName(requestParam.value());
						if(!requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE)){
							paramType.setDefaultValue(requestParam.defaultValue());
							paramType.setRequired(false);
						}else{
							paramType.setRequired(requestParam.required());
						}
					}else if(annotation instanceof PathVariable){
						pathVariable = (PathVariable)annotation;
						paramType.setName(pathVariable.value());
						paramType.setRequired(true);
					}
				}
				if(requestParam==null&& pathVariable == null){
					throw new RuntimeException(clazz.getSimpleName()+"."+method.getName()+" param["+i+"] must be annotation present RequestParam or PathVariable");
				}
			}
			paramType.setType(type);
			this.requestParamTypes[i] = paramType;
		}
	}

	public Object invoke(HttpRequest request,HttpResponse response) throws Throwable{
		try{
			if(this.parameterLength>0){
				Object[] params = new Object[this.parameterLength];
				for(int i=0;i<this.parameterLength;i++){
					RequestParamType requestParamType = requestParamTypes[i];
					Type type = requestParamType.getType();
					if(type==Type.HTTP_REQUEST){
						params[i]=request;
					}else if(type == Type.HTTP_RESPONSE){
						params[i]=response;
					}else{
						String name = requestParamType.getName();
						String value = request.getParameter(name);
						if(value==null){
							value = requestParamType.getDefaultValue();
						}
						if(requestParamType.isRequired()&&value==null){
							logger.warn("bad request http param[{}]=null",name);
							throw new HttpException(HttpResponseStatus.BAD_REQUEST);
						}
						if(value==null){
							continue;
						}
						switch(type){
						case STRING:params[i]=value;break;
						case LIST:params[i]=objectMapper.readValue(value, List.class);break;
						case SET:params[i]=objectMapper.readValue(value, Set.class);break;
						case MAP:params[i]=objectMapper.readValue(value, Map.class);break;
						case ARRARY:params[i]=objectMapper.readValue(value, List.class).toArray();break;
						case BOOLEAN:params[i]=Boolean.parseBoolean(value);break;
						case SHORT:params[i]=Short.parseShort(value);break;
						case INTEGER:params[i]=Integer.parseInt(value);break;
						case LONG:params[i]=Long.parseLong(value);break;
						case FLOAT:params[i]=Float.parseFloat(value);break;
						case DOUBLE:params[i]=Double.parseDouble(value);break;
						case CHAR:params[i]=value.charAt(0);break;
						case BYTE:params[i]=value.getBytes()[0];break;
						default:{}
						}
					}
				}
				return method.invoke(object,params);
			}else{
				return method.invoke(object);
			}
		}catch(InvocationTargetException e){
			throw e.getTargetException();
		}
	}
	public boolean isResponseBody() {
		return isResponseBody;
	}
	public boolean isXssFilter(){
		return xssFilter;
	}
}
