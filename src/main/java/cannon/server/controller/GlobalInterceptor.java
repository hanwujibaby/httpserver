package cannon.server.controller;

import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;


public interface GlobalInterceptor {
	boolean beforeHandle(HttpRequest request, HttpResponse response, Object handler)throws Exception;
	void afterHandle(HttpRequest request, HttpResponse response, Object handler)throws Exception;
}
