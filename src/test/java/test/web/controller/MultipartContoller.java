package test.web.controller;

import java.io.File;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import cannon.server.controller.RequestMapping;
import cannon.server.http.HttpRequest;

@Controller
public class MultipartContoller {
	private static Logger logger = LoggerFactory.getLogger(MultipartContoller.class);
	@RequestMapping("/multipart.do")
	public String multipart(HttpRequest request) throws Exception{
		FileItem file = request.getFile("file");
		logger.info(file.getFieldName()+":"+file.getFieldName()+":"+file.getSize());
		FileUtils.writeByteArrayToFile(new File("D://test.jpg"), file.get());
		return "success";
	}
}
