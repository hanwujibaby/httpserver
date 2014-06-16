package cannon.server.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.RequestContext;

public class HttpRequestContext implements RequestContext{
	private HttpRequest request;
	
	public HttpRequestContext(HttpRequest request) {
		this.request = request;
	}
	
	@Override
	public String getCharacterEncoding() {
		return request.getCharacterEncoding();
	}

	@Override
	public String getContentType() {
		return request.getContentType();
	}

	@Override
	public int getContentLength() {
		return request.getContentLength();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return request.getInputStream();
	}

}
