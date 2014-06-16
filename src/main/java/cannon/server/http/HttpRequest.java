package cannon.server.http;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo Request 对外接口类，实现业务的时候只需操作这个接口即可
 */
public interface HttpRequest extends HttpMessage{
    HttpMethod getMethod();
    String getUri();
    String getQueryString();
    String getParameter(String name);
    Map<String,String> getParametersMap();
    InputStream getInputStream();
    String getClientIp();
    int getContentLength();
    String getContentType();
    String getCharacterEncoding();
    FileItem getFile(String name);
}
