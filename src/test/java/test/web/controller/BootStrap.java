package test.web.controller;


import java.net.URL;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.core.HttpServer;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-8
 * @qq 271398203
 * @todo Http Server 启动类
 */
public final class BootStrap {
	private static Logger logger = LoggerFactory.getLogger(BootStrap.class);
	public static void main(String[] args) throws Exception{
		URL url = BootStrap.class.getResource("/log4j.properties");
		PropertyConfigurator.configure(url);
		HttpServer server = new HttpServer();
		try{
			server.startup();
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			server.shutdown();
		}
	}
}
