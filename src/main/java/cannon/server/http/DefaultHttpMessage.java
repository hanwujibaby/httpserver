package cannon.server.http;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;


public class DefaultHttpMessage implements HttpMessage{
	protected final HttpVersion version;
	protected final HttpHeaders headers = new HttpHeaders();
	public DefaultHttpMessage(HttpVersion version){
		this.version = version;
	}
	@Override
	public String getHeader(String name) {
		return headers.getHeader(name);
	}

	@Override
	public List<String> getHeaders(String name) {
		return headers.getHeaders(name);
	}

	@Override
	public List<Entry<String, String>> getHeaders() {
		return headers.getHeaders();
	}

	@Override
	public boolean containsHeader(final String name) {
		return headers.containsHeader(name);
	}

	@Override
	public Set<String> getHeaderNames() {
		return headers.getHeaderNames();
	}

	@Override
	public HttpVersion getProtocolVersion() {
		return version;
	}

	@Override
	public void addHeader(final String name, final Object value) {
		headers.addHeader(name, value);
	}

	@Override
	public void setHeader(String name, Object value) {
		headers.setHeader(name, value);
	}

	@Override
	public void setHeader(final String name, final Iterable<?> values) {
		headers.setHeader(name, values);
	}

	@Override
	public void removeHeader(final String name) {
		headers.removeHeader(name);
	}

	@Override
	public void clearHeaders() {
		headers.clearHeaders();
	}

}
