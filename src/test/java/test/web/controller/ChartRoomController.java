package test.web.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;
import cannon.server.http.HttpException;
import cannon.server.http.HttpRequest;
import cannon.server.http.HttpResponse;
import cannon.server.http.HttpResponseStatus;

@Controller
public class ChartRoomController {
	Map<String,ChartSession> sessions = new ConcurrentHashMap<String,ChartSession>();
	@RequestMapping("/websocket.do")
	public Object websocket(HttpRequest request,HttpResponse response)throws Exception{
		String name = request.getParameter("name");
		ChartSession newSession = new ChartSession(name,sessions);
		if(response.upgrade(request, newSession)){
			ChartSession oldSession = sessions.get(name);
			if(oldSession!=null){
				oldSession.sendText("你已经在别处登录，此会话已被强制下线".getBytes("UTF-8"), 2, TimeUnit.SECONDS, null).get();
				oldSession.close(null, 1, TimeUnit.SECONDS);
			}
			sessions.put(name, newSession);
			return null;
		}else{
			throw new HttpException(HttpResponseStatus.FORBIDDEN);
		}
	}
}
