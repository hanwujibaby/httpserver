package test.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.controller.GlobalInterceptor;
import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;

public class TestInterceptor implements GlobalInterceptor {
	Logger logger = LoggerFactory.getLogger(TestInterceptor.class);
	@Override
	public boolean beforeHandle(HttpRequest request,
			HttpResponse response, Object handler) throws Exception {
		logger.info("interceptor before handle");
		return true;
	}

	@Override
	public void afterHandle(HttpRequest request,
			HttpResponse response, Object handler) throws Exception {
		logger.info("interceptor after handle");
	}

}
