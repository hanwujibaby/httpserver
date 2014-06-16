package cannon.server.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.core.SocketSession;
import cannon.server.util.HttpCodecUtil;
import cannon.server.util.ServerConfig;



/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-13
 * @qq 271398203
 * @todo HTTP Response Encode And Decode Class
 */
public class HttpMessageSerializer{
	protected Logger logger = LoggerFactory.getLogger(HttpMessageSerializer.class);

	private int maxInitialLineLength = 1024*2; //Default 2KB
	private int maxHeaderSize = 1024*4; //Default 4KB
	private int maxContextSize = 1024*1024*5 ;//Default 5MB
	private String charset = "UTF-8";
	private String dynamicSuffix;
	private String defaultIndex;

	public HttpMessageSerializer(ServerConfig serverConfig){
		this.charset = serverConfig.getString("server.http.charset", charset);
		logger.info("server.http.charset : {}",charset);
		this.maxHeaderSize = serverConfig.getBytesLength("server.http.maxHeaderSize", this.maxHeaderSize);
		logger.info("server.http.maxHeaderSize : {}",maxHeaderSize);

		this.maxContextSize = serverConfig.getBytesLength("server.http.maxContextSize", this.maxContextSize);
		logger.info("server.http.maxContextSize : {}",maxContextSize);

		this.dynamicSuffix = serverConfig.getString("server.http.dynamic.suffix", ".do");
		logger.info("server.http.dynamic.suffix : {}",dynamicSuffix);
		
		this.defaultIndex = serverConfig.getString("server.http.index", ".html");
		logger.info("server.http.dynamic.suffix : {}",this.defaultIndex);
	}

	public boolean decode(ByteBuffer buffer,HttpProcessor processor)throws Exception{
		boolean finished = false;
		DefaultHttpRequest request = null;
		try{
			buffer.flip();
			HttpSocketStatus status = processor.getSocketStatus();
			request = processor.getRequest();
			switch(status){
			case SKIP_CONTROL_CHARS: {
				skipControlCharacters(buffer);
				processor.setSocketStatus(HttpSocketStatus.READ_INITIAL);
			}
			case READ_INITIAL:{
				String line = readLine(buffer,maxInitialLineLength);
				if(line==null){
					break;
				}
				String[] initialLine = splitInitialLine(line);
				String text = initialLine[0].toUpperCase();
				HttpMethod method = HttpMethod.getHttpMethod(text);
				if(method==null){
					throw new HttpException(HttpResponseStatus.METHOD_NOT_ALLOWED, "Unsuported HTTP Method "+text);
				}
				String uri = initialLine[1];
				text = initialLine[2].toUpperCase();
				HttpVersion version;
				if (text.equals("HTTP/1.1")) {
					version=HttpVersion.HTTP_1_1;
				}else if (text.equals("HTTP/1.0")) {
					version=HttpVersion.HTTP_1_0;
				}else{
					throw new HttpException(HttpResponseStatus.BAD_REQUEST,"Unsuported HTTP Protocol "+text);
				}
				request = new DefaultHttpRequest(version,method,uri);
				request.setCharacterEncoding(charset);
				int at = uri.indexOf('?');
				String queryString ;
				if(at>=0){
					queryString = uri.substring(0, at);
				}else{
					queryString = uri;
				}
				if(queryString.endsWith("/")){
					queryString = queryString+this.defaultIndex;
					request.setQueryString(queryString);
				}else{
					request.setQueryString(queryString);
				}
				
				if(queryString.endsWith(this.dynamicSuffix)){
					request.setDynamic(true);
					if(at>0){
						String params = uri.substring(at);
						request.decodeContentAsURL(params,charset);
					}
				}else{
					request.setDynamic(false);
				}
//				logger.debug("Socket Session({}) : {}",processor.hashCode(),queryString);
				processor.setRequest(request);
				processor.setSocketStatus(HttpSocketStatus.READ_HEADER);
			}
			case READ_HEADER:{
				if(!readHeaders(buffer,request)){
					break;
				}
				long contentLength = HttpHeaders.getContentLength(request, -1);
				if(request.isDynamic()){
					if(contentLength>0){
						if(contentLength>this.maxContextSize){
							throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, "Request Entity Too Large : "+contentLength);
						}
						try {
							request.createContentBuffer((int)contentLength,request.getHeader(HttpHeaders.Names.CONTENT_TYPE));
						} catch (IOException e) {
							logger.info(e.getMessage(),e);
							throw new HttpException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
						}
						processor.setSocketStatus(HttpSocketStatus.READ_VARIABLE_LENGTH_CONTENT);
					}else{
						processor.setSocketStatus(HttpSocketStatus.RUNNING);
						finished=true;
						break;
					}
				}else{
					if(contentLength>0){
						throw new HttpException(HttpResponseStatus.BAD_REQUEST,"Http Static Request Do Not Suport Content Length : " + contentLength);
					}else{
						processor.setSocketStatus(HttpSocketStatus.RUNNING);
						finished=true;
						break;
					}
				}
			}
			case READ_VARIABLE_LENGTH_CONTENT:{
				try {
					if(request.readContentBuffer(buffer)){
						processor.setSocketStatus(HttpSocketStatus.RUNNING);
						finished=true;
					}
				} catch (IOException e) {
					logger.info(e.getMessage(),e);
					throw new HttpException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
				}
				break;
			}
			default:throw new HttpException(HttpResponseStatus.BAD_REQUEST,"Error Scoket Status : " + status);
			}
		}catch(Exception e){
			if(request!=null){
				request.destroy();
			}
			throw e;
		}finally{
			if(buffer!=null){
				buffer.compact();
			}
		}
		return finished;
	}
	
	
	

	public void encodeInitialLine(ByteBuffer buffer,HttpResponse response) throws IOException{
		byte[] bytes = response.getProtocolVersion().toString().getBytes(charset);
		buffer.put(bytes);
		buffer.put(HttpCodecUtil.SP);
		buffer.put(response.getStatus().getBytes());
		buffer.put(HttpCodecUtil.CRLF);
	}
	public void encodeHeaders(ByteBuffer buffer,HttpResponse response,SocketSession session) throws IOException, InterruptedException, ExecutionException {
		int remaining = buffer.remaining();
		
		for(Entry<String,String> header : response.getHeaders()){
			byte[] key = header.getKey().getBytes(charset);
			byte[] value = header.getValue().getBytes(charset);
			remaining-=key.length+value.length+3;
			if(remaining<=0){
				buffer.flip();
				session.write(buffer).get();
				remaining = buffer.remaining();
				buffer.compact();
			}
			buffer.put(key);
			buffer.put(HttpCodecUtil.COLON_SP);
			buffer.put(value);
			buffer.put(HttpCodecUtil.CRLF);
		}
		if(remaining<=0){
			session.write(buffer).get();
		}
		buffer.put(HttpCodecUtil.CRLF);
	}
	
	
	public String getCharset() {
		return charset;
	}

	private boolean readHeaders(ByteBuffer buffer,HttpRequest request) throws HttpException {

		StringBuilder sb = new StringBuilder(64);
		int limit = buffer.limit();
		int position = buffer.position();
		int lineLength = 0;
		for(int index=position;index<limit;index++){
			byte nextByte = buffer.get(index);
			if (nextByte == HttpConstants.CR) {
				nextByte = buffer.get(index+1);
				if (nextByte == HttpConstants.LF) {
					buffer.position(index);
					if(lineLength==0){
						buffer.position(index+2);
						return true;
					}else{
						buffer.position(index);
					}
					readHeader(request,sb.toString());
					lineLength=0;
					sb.setLength(0);
					index++;
				}
			}else if (nextByte == HttpConstants.LF) {
				if(lineLength==0){
					buffer.position(index+2);
					return true;
				}else{
					buffer.position(index);
				}
				readHeader(request,sb.toString());
				lineLength=0;
				sb.setLength(0);
				index++;
			}else{
				if (lineLength >= maxHeaderSize) {
					throw new HttpException(HttpResponseStatus.BAD_REQUEST,"An HTTP header is larger than " + maxHeaderSize +" bytes.");
				}
				lineLength ++;
				sb.append((char) nextByte);
			}
		}
		return false;
	}

	private static void readHeader(HttpRequest request,String header){
		String[] kv = splitHeader(header);
		request.addHeader(kv[0], kv[1]);
	}
	
	private static String[] splitHeader(String sb) {
		final int length = sb.length();
		int nameStart;
		int nameEnd;
		int colonEnd;
		int valueStart;
		int valueEnd;

		nameStart = findNonWhitespace(sb, 0);
		for (nameEnd = nameStart; nameEnd < length; nameEnd ++) {
			char ch = sb.charAt(nameEnd);
			if (ch == ':' || Character.isWhitespace(ch)) {
				break;
			}
		}

		for (colonEnd = nameEnd; colonEnd < length; colonEnd ++) {
			if (sb.charAt(colonEnd) == ':') {
				colonEnd ++;
				break;
			}
		}

		valueStart = findNonWhitespace(sb, colonEnd);
		if (valueStart == length) {
			return new String[] {
					sb.substring(nameStart, nameEnd),
					""
			};
		}

		valueEnd = findEndOfString(sb);
		return new String[] {
				sb.substring(nameStart, nameEnd),
				sb.substring(valueStart, valueEnd)
		};
	}
	private static String readLine(ByteBuffer buffer, int maxLineLength) throws HttpException {
		StringBuilder sb = new StringBuilder(64);
		int lineLength = 0;
		int limit = buffer.limit();
		int position = buffer.position();
		for(int index=position;index<limit;index++){
			byte nextByte = buffer.get(index);
			if (nextByte == HttpConstants.CR) {
				nextByte = buffer.get(index+1);
				if (nextByte == HttpConstants.LF) {
					buffer.position(index+2);
					return sb.toString();
				}
			}else if (nextByte == HttpConstants.LF) {
				buffer.position(index+2);
				return sb.toString();
			}else{
				if (lineLength >= maxLineLength) {
					throw new HttpException(HttpResponseStatus.REQUEST_URI_TOO_LONG,"An HTTP line is larger than " + maxLineLength +" bytes.");
				}
				lineLength ++;
				sb.append((char) nextByte);
			}
		}
		return null;
	}
	private static String[] splitInitialLine(String sb) {
		int aStart;
		int aEnd;
		int bStart;
		int bEnd;
		int cStart;
		int cEnd;

		aStart = findNonWhitespace(sb, 0);
		aEnd = findWhitespace(sb, aStart);

		bStart = findNonWhitespace(sb, aEnd);
		bEnd = findWhitespace(sb, bStart);

		cStart = findNonWhitespace(sb, bEnd);
		cEnd = findEndOfString(sb);

		return new String[] {
				sb.substring(aStart, aEnd),
				sb.substring(bStart, bEnd),
				cStart < cEnd? sb.substring(cStart, cEnd) : "" };
	}
	private static int findNonWhitespace(String sb, int offset) {
		int result;
		for (result = offset; result < sb.length(); result ++) {
			if (!Character.isWhitespace(sb.charAt(result))) {
				break;
			}
		}
		return result;
	}
	private static int findWhitespace(String sb, int offset) {
		int result;
		for (result = offset; result < sb.length(); result ++) {
			if (Character.isWhitespace(sb.charAt(result))) {
				break;
			}
		}
		return result;
	}
	private static int findEndOfString(String sb) {
		int result;
		for (result = sb.length(); result > 0; result --) {
			if (!Character.isWhitespace(sb.charAt(result - 1))) {
				break;
			}
		}
		return result;
	}
	private static void skipControlCharacters(ByteBuffer buffer) {
		int limit = buffer.limit();
		int position = buffer.position();
		for(int index=position;index<limit;index++){
			char c = (char) (buffer.get(index) & 0xFF);
			if (!Character.isISOControl(c) &&
					!Character.isWhitespace(c)) {
				buffer.position(index);
				break;
			}
		}
	}
}
