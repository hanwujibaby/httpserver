/*
 * 数据传输报文格式详细描述，据说这个图表是权威的。
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-------+-+-------------+-----------------------------+
 * |F|R|R|R| opcode|M| Payload len | Extended payload length     |
 * |I|S|S|S| (4)   |A| (7)         |          (16/64)            |
 * |N|V|V|V|       |S|             | (if payload len==126/127)   |
 * | |1|2|3|       |K|             |                             |
 * +-+-+-+-+-------+-+-------------+-----------------------------+
 * |   Extended payload length continued, if payload len == 127  |
 * +-----------------------------+-------------------------------+
 * |                Masking-key, if MASK set to 1                |
 * +-------------------------------+-----------------------------+
 * |   Masking-key (continued)     |        Payload Data         |
 * +-------------------------------+-----------------------------+
 * |                Payload Data continued ...                   |
 * +-------------------------------+-----------------------------+
 * 
 * 
 * 
 * 数据帧示例
 * 
 * 1.未掩码文件消息的单个帧
 * 0x81 0x05 0x48 0x65 0x6c 0x6c 0x6f (包含 "Hello")
 * 
 * 2.掩码的文本消息的单个帧
 * 0x81 0x85 0x37 0xfa 0x21 0x3d 0x7f 0x9f 0x4d 0x51 0x58 (包含 "Hello")
 * 
 * 3.一个分片的未掩码的文本消息
 * 0x01 0x03 0x48 0x65 0x6c (包含 "Hel")
 * 0x80 0x02 0x6c 0x6f (包含 "lo")
 * 
 * 4.未掩码的Ping请求和掩码的Ping响应
 * 0x89 0x05 0x48 0x65 0x6c 0x6c 0x6f
 * (包含内容体"Hello"、但内容体的内容是随意的)
 * 0x8a 0x85 0x37 0xfa 0x21 0x3d 0x7f 0x9f 0x4d 0x51 0x58
 * (包含内容体"Hello"、匹配ping的内容体)
 * 
 * 5.单个未掩码帧中的256字节的二进制消息
 * 0x82 0x7E 0x0100 [256字节的二进制数据]
 * 
 * 6.单个未掩码帧中的64KB的二进制消息
 * 0x82 0x7F 0x0000000000010000 [65536字节的二进制数据]
 * 
 * 
 * 注意：客户端发送到服务端的帧必须掩码，如果服务器检测到做掩码操作，服务端应当立即关闭连接。在这种情况下服务端可以发送一个Close帧
 *     服务端发送的所有帧必须不掩码，如果服务端发送的数据掩码了，客户端 应当立即关闭连接
 */
package cannon.server.websocket;

import java.nio.ByteBuffer;

/**
 * @author fangjialong
 * @name 房佳龙
 * @date 2014-1-11
 * @qq 271398203
 * 
 */
public interface WebSocket {
	void setWebSocketSession(WebSocketSession session);
	void onInit(boolean endFragment,long payloadLength) throws WebSocketException;
	void onRead(ByteBuffer buffer,int length) throws WebSocketException;
	void onComplete() throws WebSocketException;
	void onClose() throws WebSocketException;
}
