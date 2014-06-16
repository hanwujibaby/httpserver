package cannon.server.http;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo Http 消息抽象接口，HttpRequest、HttpResponse 都继承了这个接口
 */
public interface HttpMessage {
    String getHeader(String name);
    List<String> getHeaders(String name);
    List<Map.Entry<String, String>> getHeaders();
    boolean containsHeader(String name);
    Set<String> getHeaderNames();
    HttpVersion getProtocolVersion();
    void addHeader(String name, Object value);
    void setHeader(String name, Object value);
    void setHeader(String name, Iterable<?> values);
    void removeHeader(String name);
    void clearHeaders();
}
