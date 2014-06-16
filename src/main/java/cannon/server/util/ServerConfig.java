package cannon.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-12
 * @qq 271398203
 * @todo Http Server Config Class
 */
public class ServerConfig {
	private static Logger logger = LoggerFactory.getLogger(ServerConfig.class);
	private Properties proerties = new Properties();
	public ServerConfig(){
		logger.info("load properties classpath:config.properties");
		InputStream in = null;
		try{
			in = ServerConfig.class.getClassLoader().getResourceAsStream("server.properties");
			proerties.load(in);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}finally{
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}
	
	public String getString(String key,String defaultValue){
		return proerties.getProperty(key, defaultValue);
	}
	public int getInteger(String key,int defaultValue){
		String str = proerties.getProperty(key);
		int rs;
		if(str!=null){
			rs = Integer.parseInt(str);
		}else{
			rs = defaultValue;
		}
		return rs;
	}
	public long getLong(String key,int defaultValue){
		String str = proerties.getProperty(key);
		long rs;
		if(str!=null){
			rs = Long.parseLong(str);
		}else{
			rs = defaultValue;
		}
		return rs;
	}
	public boolean getBoolean(String key,boolean defaultValue){
		String str = proerties.getProperty(key);
		boolean rs;
		if(str!=null){
			rs = Boolean.parseBoolean(str);
		}else{
			rs = defaultValue;
		}
		return rs;
	}
	public int getBytesLength(String key,int defaultValue){
		String str = proerties.getProperty(key);
		if(str!=null){
			if(str.toUpperCase().endsWith("MB")){
				return Integer.parseInt(str.substring(0,str.length()-2))*1024*1024;
			}else if(str.toUpperCase().endsWith("KB")){
				return Integer.parseInt(str.substring(0,str.length()-2))*1024;
			}else if(str.toUpperCase().endsWith("B")){
				return Integer.parseInt(str.substring(0,str.length()-1));
			}else{
				throw new IllegalArgumentException(key+" : "+str);
			}
		}else{
			return defaultValue;
		}
	}
}
