package cannon.server.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo HttpRequest 实现类，用于保存HTTP请求栈上数据
 */
public final class DefaultHttpRequest extends DefaultHttpMessage implements HttpRequest {
	private static final Logger logger = LoggerFactory.getLogger(DefaultHttpRequest.class);
	private final HttpMethod method;
	private final String uri;
	private String queryString;
	private Map<String,String> parameters;
	private Map<String,FileItem> files;
	private byte[] content;
	private int contentLength;
	private int contentIndex;
	private InputStream inputStream;
	private boolean dynamic;
	private String clientIp;
	protected String characterEncoding;
	private String contentType;
	private long start = 0;
	
	/*
	 * 1:form urlencode submit,2:multipart submit
	 */
	private int initialType = 0;
	private File bufferFile;
	private BufferedOutputStream bufferFileOutputStream;

	void createContentBuffer(int contentLength,String contentType) throws IOException {
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.contentIndex = 0;
		if(contentType!=null){
			String lower = this.contentType.toLowerCase();
			if(lower.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)){
				initialType = 1;
				content = new byte[contentLength];
				return ;
			}else if(lower.startsWith("multipart/")){
				initialType = 2;
			}
		}
		bufferFile = File.createTempFile("request_", ".tmp");
		bufferFile.deleteOnExit();
		bufferFileOutputStream = new BufferedOutputStream (new FileOutputStream(bufferFile));
	}

	boolean readContentBuffer(ByteBuffer buffer) throws Exception{
		int remain = buffer.remaining();
		boolean finished = false;
		if(initialType==1){
			for(;this.contentIndex<this.contentLength&&remain>0;this.contentIndex++,remain--){
				content[this.contentIndex] = buffer.get();
			}
			finished = this.contentIndex == this.contentLength;
		}else{
			for(;this.contentIndex<this.contentLength&&remain>0;this.contentIndex++,remain--){
				this.bufferFileOutputStream.write(buffer.get());
			}
			finished = this.contentIndex == this.contentLength;
		}
		return finished;
	}

	void initialize() throws Exception{
		if(initialType==1){
			decodeContentAsURL(new String(this.content),this.characterEncoding);
			this.content = null;
		}else{
			if(this.bufferFileOutputStream!=null){
				//close函数里自带flush,无需重复flush()
				this.bufferFileOutputStream.close();
				this.bufferFileOutputStream = null;
			}
			if(this.bufferFile!=null){
				this.inputStream = new FileInputStream(this.bufferFile);
			}
		}

		if(initialType==2&&contentType!=null&&contentType.startsWith("multipart/")){
			FileUpload fileUpload = new FileUpload(new DiskFileItemFactory());
			List<FileItem> list = fileUpload.parseRequest(new HttpRequestContext(this));
			for(FileItem item:list){
				if(item.isFormField()){
					String value;
					try {
						value = item.getString(characterEncoding);
					} catch (UnsupportedEncodingException e) {
						logger.warn("Could not decode multipart item '"+item.getFieldName()+"' with encoding '"+characterEncoding+"': using platform default");
						value = item.getString();
					}
					addParameter(item.getFieldName(), value);
				}else{
					if(files==null){
						files = new HashMap<String,FileItem>();
					}
					files.put(item.getFieldName(), item);
				}
			}
		}
		
	}

	/**
	 * 释放文件资源
	 */
	public void destroy(){
		if(this.inputStream!=null){
			try{
				this.inputStream.close();
			}catch(IOException e){}
		}
		if(this.bufferFile!=null){
			this.bufferFile.delete();
		}
		this.parameters = null;
		if(files!=null){
			for(FileItem file:files.values()){
				file.delete();
			}
			files = null;
		}
		if(logger.isDebugEnabled()){
			logger.debug("http request process delay:{}ms",(System.currentTimeMillis()-start));
		}
		
	}
	
	public boolean isDynamic() {
		return dynamic;
	}
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}
	public DefaultHttpRequest(HttpVersion version, HttpMethod method, String uri){
		super(version);
		this.method = method;
		this.uri = uri;
		
		if(logger.isDebugEnabled()){
			start = System.currentTimeMillis();
		}
	}
	void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	void setParamsString(String params){
		int start = 0;
		int length = params.length();
		//跳过无效字符 '?'
		for(;start<length;start++){
			if('?'!=params.charAt(start)){
				break;
			}
		}
		int left=start;
		int middle=0;
		for(;start<length;start++){
			if('='==params.charAt(start)){
				middle = start;
				for(;start<length;start++){
					char c = params.charAt(start);
					if('&'==c){
						String key = params.substring(left,middle);
						String value = params.substring(middle+1,start);
						addParameter(key,value);
						//跳过无效字符 '&'
						for(;start<length;start++){
							if('&'!=params.charAt(start)){
								break;
							}
						}
						left=start;
						break;
					}
				}
			}
		}
		if(middle>left){
			String key = params.substring(left,middle);
			String value = params.substring(middle+1);
			addParameter(key,value);
		}
	}

	void decodeContentAsURL(String params,String charset){
		int start = 0;
		int length = params.length();
		//跳过无效字符 '?'
		for(;start<length;start++){
			if('?'!=params.charAt(start)){
				break;
			}
		}
		int left=start;
		int middle=0;
		for(;start<length;start++){
			if('='==params.charAt(start)){
				middle = start;
				for(;start<length;start++){
					char c = params.charAt(start);
					if('&'==c){
						String key = params.substring(left,middle);
						String value = params.substring(middle+1,start);
						try {
							addParameter(URLDecoder.decode(key,charset),URLDecoder.decode(value,charset));
						} catch (UnsupportedEncodingException e) {
							//ignore
						}
						//跳过无效字符 '&'
						for(;start<length;start++){
							if('&'!=params.charAt(start)){
								break;
							}
						}
						left=start;
						break;
					}
				}
			}
		}
		if(middle>left){
			String key = params.substring(left,middle);
			String value = params.substring(middle+1);
			try {
				addParameter(URLDecoder.decode(key, charset),URLDecoder.decode(value, charset));
			} catch (UnsupportedEncodingException e) {
				//ignore
			}
		}

	}

	public void addParameter(String key,String value){
		if(parameters==null){
			parameters = new HashMap<String,String>();
		}
		parameters.put(key, value);
	}
	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}
	void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}
	@Override
	public HttpMethod getMethod() {
		return method;
	}
	@Override
	public String getUri() {
		return uri;
	}
	@Override
	public String getQueryString() {
		return queryString;
	}
	@Override
	public String getParameter(String name) {
		return parameters==null?null:parameters.get(name);
	}
	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	@Override
	public Map<String, String> getParametersMap() {
		return parameters==null?new HashMap<String,String>():parameters;
	}
	@Override
	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}
	@Override
	public int getContentLength() {
		return contentLength;
	}
	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public FileItem getFile(String name) {
		return files==null?null:files.get(name);
	}
}
