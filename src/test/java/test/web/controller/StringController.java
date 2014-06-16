package test.web.controller;

import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;

@Controller
public class StringController {
	
	@RequestMapping("/string.do")
	public String string(){
		return "This is a string";
	}
}
