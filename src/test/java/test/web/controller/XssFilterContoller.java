package test.web.controller;

import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;
import cannon.server.controller.XssFilter;



@Controller
public class XssFilterContoller {
	@RequestMapping("/xss.do")
	public String xss(){
		return "<script>alert('Hello World');</script>";
	}
	
	@RequestMapping("/nonexss.do")
	@XssFilter(false)
	public String nonexss(){
		return "<script>alert('Hello World');</script>";
	}
}
