package cannon.server.http;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 
 */
public enum HttpVersion {
	HTTP_1_0(false,"HTTP/1.0"),
	HTTP_1_1(true,"HTTP/1.1");
	private boolean keepAliveDefault;
	private String name; 
	HttpVersion(boolean keepAliveDefault,String name){
		this.keepAliveDefault = keepAliveDefault;
		this.name = name;
	}
	public boolean isKeepAliveDefault() {
		return keepAliveDefault;
	}
	@Override
	public String toString() {
		return name;
	}
}
