package test.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;
import cannon.server.controller.VelocityTemplate;

@Controller
public class VelocityController {
	
	@RequestMapping("/velocity.do")
	@VelocityTemplate("test.vm")
	public Map<String,Object> velocity(){
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("name", "房佳龙");
		result.put("age", 25);
		result.put("language", "Java");
		return result;
	}
}
