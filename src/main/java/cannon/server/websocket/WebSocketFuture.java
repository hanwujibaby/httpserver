package cannon.server.websocket;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WebSocketFuture implements Future<Void>{
	private boolean canceled = false;
	private boolean done = false;
	private Throwable throwable;
	private WebSocketCallback callback;
	private ReentrantLock lock = new ReentrantLock();
	private Condition notNull = lock.newCondition();
	
	
	public WebSocketFuture(WebSocketCallback callback){
		this.callback = callback;
	}
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		canceled = true;
		return canceled;
	}

	@Override
	public boolean isCancelled() {
		return canceled;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException {
		lock.lock();
		try {
			if(!done) {
				notNull.await();
			}
		} finally {
			lock.unlock();
		}
		if(throwable!=null){
			throw new RuntimeException(throwable);
		}
		return null;
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		lock.lock();
		try {
			if(!done) {
				boolean success = notNull.await(timeout, unit);
				if(!success) {
					throw new TimeoutException();
				}
			}
		} finally {
			lock.unlock();
		}
		if(throwable!=null){
			throw new RuntimeException(throwable);
		}
		return null;
	}

	public void done() {
		lock.lock();
		try {
			if(!done) {
				this.done = true;
				notNull.signalAll();
			}
		} finally {
			lock.unlock();
		}
		if(callback!=null)
			callback.success();
	}
	
	public void exception(Throwable t){
		lock.lock();
		try {
			if(!done) {
				throwable = t;
				this.done = true;
				notNull.signalAll();
			}
		} finally {
			lock.unlock();
		}
		if(callback!=null)
			callback.error(t);
	}
}
