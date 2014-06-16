package cannon.server.core;

import java.nio.ByteBuffer;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 * @todo 	ByteBuffer的一个对象池工厂，每次客户端TCP连接被创建的时候，Server就会从对象池里获取一个ByteBuffer对象
 * 			然后当TCP关闭的时候将对象返还对象
 */
public class ByteBufferFactory implements PoolableObjectFactory<ByteBuffer>{
	private final boolean direct;
	private final int capacity;

	public ByteBufferFactory(boolean direct,int capacity){
		this.direct = direct;
		this.capacity = capacity;
		
	}
	@Override
	public ByteBuffer makeObject() throws Exception {
		if(direct){
			return ByteBuffer.allocateDirect(capacity);
		}else{
			return ByteBuffer.allocate(capacity);
		}
	}

	@Override
	public void destroyObject(ByteBuffer buffer) throws Exception {
		buffer.clear();
	}

	@Override
	public boolean validateObject(ByteBuffer obj) {
		return true;
	}

	@Override
	public void activateObject(ByteBuffer obj) throws Exception {
	}

	@Override
	public void passivateObject(ByteBuffer obj) throws Exception {
		
	}
	
	

}
