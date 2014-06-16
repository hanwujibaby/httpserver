package cannon.server.controller;

import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;

public interface ExceptionHandler {
	byte[] resolveException(HttpRequest request , HttpResponse response,Throwable t);
}
