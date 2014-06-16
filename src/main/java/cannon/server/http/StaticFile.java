package cannon.server.http;


public final class StaticFile {
	private final String queryString;
	private final byte[] bytes;
	private final String lastModified;
	private final String contentType;
	public StaticFile(String queryString, byte[] bytes, String lastModified,String contentType) {
		super();
		this.queryString = queryString;
		this.bytes = bytes;
		this.lastModified = lastModified;
		this.contentType = contentType;
	}
	public String getQueryString() {
		return queryString;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public String getLastModified() {
		return lastModified;
	}
	public String getContentType() {
		return contentType;
	}
}
