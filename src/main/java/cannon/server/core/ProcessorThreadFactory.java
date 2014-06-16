package cannon.server.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-10
 * @qq 271398203
 * @todo Socket Accept Thread Facotry, 用于创建最高优先级的线程
 */
public final class ProcessorThreadFactory implements ThreadFactory{
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final String namePrefix;
	public ProcessorThreadFactory(){
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() :
			Thread.currentThread().getThreadGroup();
		namePrefix = "processor-thread-";

	}
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r,namePrefix + threadNumber.getAndIncrement(),0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.MAX_PRIORITY){
			t.setPriority(Thread.MAX_PRIORITY);
		}
		return t;
	}

}
