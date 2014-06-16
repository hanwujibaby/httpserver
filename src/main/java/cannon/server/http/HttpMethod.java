package cannon.server.http;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 
 */
public enum HttpMethod {
	GET,
	POST;
	public static HttpMethod getHttpMethod(String name){
		if(name.equals("GET")){
			return GET;
		}else if(name.equals("POST")){
			return POST;
		}else
			return null;
	}
}
