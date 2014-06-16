package cannon.server.core;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cannon.server.http.HttpMessageHandler;
import cannon.server.http.HttpMessageSerializer;
import cannon.server.http.StaticHandler;
import cannon.server.util.ServerConfig;
import cannon.server.websocket.WebSocketSerializer;


/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo Http Server Class
 */
public final class HttpServer{
	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
	private ExecutorService channelWorkers;
	private ExecutorService processWorkers;
	private AsynchronousChannelGroup workerGroup = null;
	private AsynchronousServerSocketChannel serverSocket = null;
	private GenericObjectPool<ByteBuffer> byteBufferPool = null;
	private long timeout;
	private HttpMessageHandler dynamicHandler;

	private volatile boolean initialized = false;
	private volatile boolean started = false;


	private ServerConfig serverConfig;
	private SocketAcceptHandler socketAcceptHandler;
	private SocketReadHandler socketReadHandler;
	private String name;
	private StaticHandler staticHandler;
	
	private int cacheControlMaxAge;
	private String htmlContentType;
	
	private HttpMessageSerializer httpMessageSerializer; 
	private WebSocketSerializer webSocketSerializer;
	
	public StaticHandler getStaticHandler() {
		return staticHandler;
	}
	public SocketReadHandler getSocketReadHandler() {
		return socketReadHandler;
	}


	public HttpMessageHandler getDynamicHandler() {
		return dynamicHandler;
	}
	public int getCacheControlMaxAge() {
		return cacheControlMaxAge;
	}
	public String getHtmlContentType() {
		return htmlContentType;
	}
	public ServerConfig getServerConfig() {
		return serverConfig;
	}
	
	public HttpMessageSerializer getHttpMessageSerializer() {
		return httpMessageSerializer;
	}
	
	public WebSocketSerializer getWebSocketSerializer() {
		return webSocketSerializer;
	}
	private void initialize(){
		logger.info("http-server initialize");
		this.socketAcceptHandler = new SocketAcceptHandler(this);
		this.socketReadHandler = new SocketReadHandler();
		this.serverConfig = new ServerConfig();
		
		this.name = serverConfig.getString("server.name", "unknown-name");
		logger.info("server.name : {}",this.name);
		String charset = serverConfig.getString("server.http.charset", "UTF-8");
		htmlContentType = "text/html; charset="+charset;
		cacheControlMaxAge = serverConfig.getInteger("server.http.static.expire", 0);
		
		String userDir = System.getProperty("user.dir");
		String tempDir;
		String templateDir;
		if(userDir.endsWith("/")){
			tempDir = userDir + "temp/";
			templateDir = userDir + "templates/";
		}else{
			tempDir = userDir + "/temp/";
			templateDir = userDir + "/templates/";
		}
		File dir = new File(tempDir);
		if(!dir.exists()){
			dir.mkdirs();
		}
		logger.info("temp.dir:{}",tempDir);
		System.setProperty("java.io.tmpdir", tempDir);
		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
		Velocity.setProperty(Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER, "Velocity");
		Velocity.setProperty(Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER_LEVEL, "INFO");
		Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, templateDir);
		Velocity.setProperty(Velocity.INPUT_ENCODING, charset);
		Velocity.setProperty(Velocity.OUTPUT_ENCODING, charset);
		Velocity.setProperty(Velocity.RUNTIME_LOG_REFERENCE_LOG_INVALID, "false");
		Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE, "false");
        Velocity.init();
        
        logger.info("templates dir:{}",templateDir);
		
		
		boolean staticOpen = this.serverConfig.getBoolean("server.http.static.open", Boolean.TRUE);
		if(staticOpen){
			this.staticHandler = new StaticHandler(this.serverConfig);
		}

		int buffer = serverConfig.getBytesLength("server.channel.buffer", 8192);
		boolean direct = serverConfig.getBoolean("server.channel.direct", false);
		int maxActive = serverConfig.getInteger("server.channel.maxActive", 100);
		int maxWait = serverConfig.getInteger("server.channel.maxWait", 1000);
		logger.info("server.channel.buffer : {}",buffer);
		logger.info("server.channel.direct : {}",direct);
		logger.info("server.channel.maxActive : {}",maxActive);
		logger.info("server.channel.maxWait : {}",maxWait);
		GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
		poolConfig.maxActive = maxActive;//最大活动对象数
		poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;//如果达到最大数量，就报错
		poolConfig.maxWait = maxWait;
		//这个是当没有里连接的时候还至少有几个对象在池中候着
		//poolConfig.minIdle
		//poolConfig.maxIdle
		poolConfig.testOnBorrow = false;
		poolConfig.testOnReturn = false;
		//每隔15分钟清理一次无用对象
		poolConfig.timeBetweenEvictionRunsMillis = 900000;
		//每次清理对象的时候每次检查几个对象
		poolConfig.minEvictableIdleTimeMillis = 6;
		poolConfig.testWhileIdle = false;
		//这里也可以使用SoftReferenceObjectPool，软引用代表GC可以主动回收池中对象
		byteBufferPool = new GenericObjectPool<ByteBuffer>(new ByteBufferFactory(direct,buffer),poolConfig);
		
		
		httpMessageSerializer = new HttpMessageSerializer(this.serverConfig);
		webSocketSerializer = new WebSocketSerializer();
	}

	@SuppressWarnings("unchecked")
	public synchronized void startup() throws Exception {
		if(!initialized){
			initialize();
			initialized = true;
		}

		if(started){
			return;
		}
		logger.info("http-server startup");



		int availableProcessors = Runtime.getRuntime().availableProcessors();
		channelWorkers = Executors.newFixedThreadPool(availableProcessors+1,new ProcessorThreadFactory());
		int threads = serverConfig.getInteger("server.process.workers", 0);
		logger.info("server.worker.threads : {}", threads);
		if(threads>0){
			logger.info("use fixed thread({}) pool",threads);
			processWorkers = Executors.newFixedThreadPool(threads);
		}else{
			logger.info("use cached thread pool");
			processWorkers = Executors.newCachedThreadPool();
		}
		workerGroup = AsynchronousChannelGroup.withCachedThreadPool(channelWorkers, 1);
		serverSocket = AsynchronousServerSocketChannel.open(workerGroup);
		int port = serverConfig.getInteger("server.socket.port", 80);
		int backlog = serverConfig.getInteger("server.socket.backlog", 100);
		timeout = serverConfig.getInteger("server.socket.timeout", 0);
		logger.info("server.socket.port : {}",port);
		logger.info("server.socket.backlog : {}",backlog);
		logger.info("server.socket.timeout : {}",timeout);

		serverSocket.bind(new InetSocketAddress(port), backlog);

		//这里只是为了和handler一起打日志,主要使用是在HttpMessageDecoder里
		String dynamicSuffix = serverConfig.getString("server.http.dynamic.suffix", ".do");
		logger.info("server.http.dynamic.suffix : {}",dynamicSuffix);
		String handlerClass = serverConfig.getString("server.http.dynamic.handler", null);
		Class<HttpMessageHandler> clazz = (Class<HttpMessageHandler>) Class.forName(handlerClass);
		dynamicHandler = clazz.newInstance();
		logger.info("server.http.dynamic.handler : {}",handlerClass);
		dynamicHandler.initialize(this.serverConfig);

		logger.info("http server is started",timeout);
		started = true;
		accept();

	}
	public void accept(){
		if(started){
			serverSocket.accept(null, this.socketAcceptHandler);
		}
	}

	public void execute(Runnable session){
		processWorkers.submit(session);
	}

	public long getTimeout() {
		return timeout;
	}

	public synchronized void shutdown() throws IOException {
		if(!started){
			return;
		}

		started = false;


		//关闭Socket Channel
		this.serverSocket.close();
		this.dynamicHandler.destroy();
		this.workerGroup.shutdown();
		
		//关闭线程池
		this.channelWorkers.shutdown();
		this.processWorkers.shutdown();


		this.channelWorkers = null;
		this.processWorkers = null;
		this.serverSocket = null;
		this.workerGroup = null;

		this.dynamicHandler = null;

		logger.info("http server is shutdown");
	}

	public boolean isStarted() {
		return started;
	}

	public ByteBuffer borrowObject() throws Exception{
		return byteBufferPool.borrowObject();
	}
	public void returnObject(ByteBuffer buffer) throws Exception{
		byteBufferPool.returnObject(buffer);
	}
	public String getName() {
		return name;
	}

}
