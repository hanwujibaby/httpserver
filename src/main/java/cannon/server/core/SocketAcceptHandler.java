package cannon.server.core;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 该类用于接受客户端TCP连接，如果有一个新的TCP连接，该类的completed函数将会被调用
 */
public class SocketAcceptHandler implements CompletionHandler<AsynchronousSocketChannel,Object>{
	private static final Logger logger = LoggerFactory.getLogger(SocketAcceptHandler.class);
	private HttpServer server;
	
	public SocketAcceptHandler(HttpServer server){
		this.server = server;
	}

	@Override
	public void completed(AsynchronousSocketChannel socket,
			Object obj) {
		try{
			SocketSession session = new SocketSession(socket,server);
			session.read();
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			try {
				socket.close();
			} catch (IOException e) {}
		}finally{
			server.accept();
		}
	}
	
	@Override
	public void failed(Throwable t, Object obj) {
		server.accept();
	}

}
