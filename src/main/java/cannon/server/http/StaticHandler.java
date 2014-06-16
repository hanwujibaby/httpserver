package cannon.server.http;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.util.CaseIgnoringComparator;
import cannon.server.util.ServerConfig;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-10
 * @qq 271398203
 * @todo 缓存所有静态资源
 */
public class StaticHandler extends TimerTask{
	private static final Logger logger = LoggerFactory.getLogger(StaticHandler.class);
	private final Map<String,String> contentTypes = new TreeMap<String,String>(CaseIgnoringComparator.INSTANCE);
	private Map<String,StaticFile> caches = new ConcurrentHashMap<String,StaticFile>();
	private String path;
	private Timer timer;
	public StaticHandler(ServerConfig serverConfig){
		String charset = serverConfig.getString("server.http.charset", "UTF-8");
		
		contentTypes.put("txt", "text/plain; charset="+charset);
		contentTypes.put("html", "text/html; charset="+charset);
		contentTypes.put("htm", "text/htm; charset="+charset);
		contentTypes.put("xml", "text/xml; charset="+charset);
		contentTypes.put("xhtml", "text/xhtml; charset="+charset);
		contentTypes.put("css", "text/css; charset="+charset);
		contentTypes.put("js", "text/javascript; charset="+charset);
		contentTypes.put("jpg", "image/jpeg");
		contentTypes.put("jpeg", "image/jpeg");
		contentTypes.put("png", "image/png");
		contentTypes.put("gif", "image/gif");
		
		
		
		
		String path = serverConfig.getString("server.http.static.dir", null);
		if(path==null){
			String userDir = System.getProperty("user.dir");
			if(userDir.endsWith("/")){
				path = userDir + "static/";
			}else{
				path = userDir + "/static/";
			}
		}
		File directory = new File(path);
		if(!directory.exists()){
			directory.mkdirs();
		}
		this.path = directory.getAbsolutePath();
		logger.info("static file path : {}",this.path);
		
		int check = serverConfig.getInteger("server.http.static.check", 0)*1000;
		if(check>0){
			this.timer = new Timer();
			this.timer.scheduleAtFixedRate(this, check, check);
		}
	}
	
	public StaticFile find(String queryString){
		StaticFile cache = caches.get(queryString);
		if(cache==null){
			if(queryString.startsWith("/")){
				File file = new File(path+queryString);
				if(file.exists()&&file.getAbsolutePath().startsWith(path)){
					try {
						byte[] content = FileUtils.readFileToByteArray(file);
						SimpleDateFormat dataFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
						dataFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
						String name = file.getName();
						int index = name.lastIndexOf(".");
						String contentType = null;
						if(index>0){
							String suffix = name.substring(index+1);
							contentType = contentTypes.get(suffix);
						}
						if(contentType==null){
							contentType = "application/octet-stream";
						}
						cache = new StaticFile(queryString, content, dataFormat.format(new Date(file.lastModified())),contentType);
						caches.put(queryString, cache);
					} catch (IOException e) {
						return null;
					}
				}else{
					return null;
				}
			}
		}
		return cache;
	}

	@Override
	public void run() {
		Iterator<Entry<String, StaticFile>> iterator = caches.entrySet().iterator();
		SimpleDateFormat dataFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
		dataFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		while(iterator.hasNext()){
			Entry<String, StaticFile> entry = iterator.next();
			String queryString = entry.getKey();
			File file = new File(path+queryString);
			if(file.exists()){
				StaticFile staticFile = entry.getValue();
				String lastModified = dataFormat.format(new Date(file.lastModified()));
				if(!lastModified.equals(staticFile.getLastModified())){
					try{
						byte[] content = FileUtils.readFileToByteArray(file);
						String name = file.getName();
						int index = name.lastIndexOf(".");
						String contentType = null;
						if(index>0){
							String suffix = name.substring(index+1);
							contentType = contentTypes.get(suffix);
						}
						if(contentType==null){
							contentType = "application/octet-stream";
						}
						StaticFile newFile = new StaticFile(queryString, content, dataFormat.format(new Date(file.lastModified())),contentType);
						caches.put(queryString, newFile);
					}catch(Exception e){
						iterator.remove();
					}
				}
			}else{
				iterator.remove();
			}
		}
	}
}
