package cannon.server.websocket;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author fangjialong
 * @name 房佳龙
 * @date 2014-1-11
 * @qq 271398203
 * @tudo 对WebSocket暴露的一些能够用来Server Push的方法
 * 		 这里该接口将websocket frame opcode做了一层简化
 * 		opcode总共有4bits用来标识负载数据帧的类型
 * 		%x0:代表一个继续帧
 * 	    %x1 代表一个文本帧
 *      %x2 代表一个二进制帧
 *      %x3-7 保留用于未来的非控制帧
 *      %x8 代表连接关闭
 *      %x9 代表ping
 *      %xA 代表pong
 *      %xB-F 保留用于未来的控制帧
 *      
 *      以上只有1、2、8三个类型的负载数据帧用得比较常用，所以得出该接口
 *      0：继续帧用于未知包体长度情况下分片时用的，这种业务场景应该是极少的
 *      1：文本帧，这个用得最广
 *      2：二进制，这个应该是JavaScript发送一个Blob对象到服务器过来
 *      8：关闭
 *      9：ping帧，可以充当一个KeepAlive，也可以作为验证远程端点仍可响应的手段。
 *      A:pong帧，服务器接受到了一个ping帧后的一个反馈，也可以是由服务器发起一个单向心跳
 */
public interface WebSocketSession {
	Future<Void> sendText(byte[] bytes,long timeout,TimeUnit unit,WebSocketCallback callback)throws InterruptedException;
	Future<Void> sendBinary(byte[] bytes,long timeout,TimeUnit unit,WebSocketCallback callback)throws InterruptedException;
	Future<Void> close(WebSocketCallback callback,long timeout,TimeUnit unit)throws InterruptedException;
}
