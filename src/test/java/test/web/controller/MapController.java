package test.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;
import cannon.server.controller.RequestParam;
import cannon.server.controller.ResponseBody;


@Controller
public class MapController {
	@RequestMapping("/map.do")
	@ResponseBody
	public Object map(
			@RequestParam(value="id")String id,
			@RequestParam(value="name",defaultValue="Unknown")String name){
		Map<String,String> result = new HashMap<String,String>();
		result.put("id", id);
		result.put("name", name);
		return result;
	}
}
