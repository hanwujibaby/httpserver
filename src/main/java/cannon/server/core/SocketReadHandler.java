package cannon.server.core;

import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 该类负责在AIO Socket上监听可读事件，如果有可读的Socket，该方法的Completed函数将会被回调
 */
public class SocketReadHandler implements CompletionHandler<Integer,SocketSession>{
	private static final Logger logger = LoggerFactory.getLogger(SocketReadHandler.class);
	
	@Override
	public void completed(Integer result,
			SocketSession session) {
		if(result==-1){
			/*
			 * 如果客户端关闭了TCP连接，这儿会返回一个-1
			 */
			session.close();
			return;
		}
		try{
			session.process();
		}catch(Throwable t){
			logger.error(t.getMessage(),t);
			session.close();
		}
	}
	@Override
	public void failed(Throwable t, SocketSession session) {
		session.close();
	}

}
