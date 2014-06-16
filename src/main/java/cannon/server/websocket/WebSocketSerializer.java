package cannon.server.websocket;

import java.nio.ByteBuffer;

public class WebSocketSerializer {
	public boolean decode(ByteBuffer buffer,WebSocketProcessor processor)throws WebSocketException{
		boolean finished = false;
		
		try{
			buffer.flip();
			int index = buffer.position();
			switch(processor.getStatus()){
			case READ_HEAD:{
				try{
					byte byte1 = buffer.get(index++);
					boolean endFragment = true;
					if((byte1&0B10000000)==0){
						//该帧表示数据帧继续，意思是数据被分片，还有后续消息
						endFragment = false;
					}
					if((byte1&0B01000000)!=0){
						//协议表示：必须是0，除非协商其他
						throw new WebSocketException("websocket RSV1 bit must be 1");
					}
					if((byte1&0B00100000)!=0){
//						//协议表示：必须是0，除非协商其他
						throw new WebSocketException("websocket RSV2 bit must be 1");
					}
					if((byte1&0B00010000)!=0){
//						//协议表示：必须是0，除非协商其他
						throw new WebSocketException("websocket RSV3 bit must be 1");
					}
					
					int opcode = byte1&0XF;
					if(opcode!=0&&opcode!=1&&opcode!=2&&opcode!=8){
//						//其他code不支持，具体理由请查看WebSocketSession类头注解
						throw new WebSocketException("websocket opcode "+opcode+" is not suported");
					}
					byte byte2 = buffer.get(index++);
					
					if((byte2&0B10000000)==0){
						//客户端必须掩码
						throw new WebSocketException("websocket message must be masked");
					}

					long payloadLength = byte2&0B01111111;
					if(payloadLength==127){
						//虽然这个数字是无符号的，但是Java语言没有支持那么大的数字 ^_^ ... ,话说我写的WebSocket不支持这么大的数据发送，因为大数据发送需要考虑到数据分片，这样代码逻辑就复杂了，而且用处不大
						payloadLength = buffer.getLong(index);
						index+=8;
						if(payloadLength<0){
							throw new WebSocketException("websocket message is too big");
						}
					}else if(payloadLength==126){
						payloadLength = ( ( buffer.get(index) & 0xff ) << 8 ) +( buffer.get(index+1) & 0xff ) ;
						index+=2;
					}
					byte[] mask = new byte[4];
					for(int i=0;i<4;i++,index++){
						mask[i] = buffer.get(index);
					}
					buffer.position(index);
					//包头解析完毕...
					processor.init(endFragment,opcode,mask,payloadLength);
					if(payloadLength==0){
						finished=true;
						processor.setStatus(WebSocketStatus.RUNNING);
						break;
					}else{
						processor.setStatus(WebSocketStatus.READ_DATA);
					}
					
				}catch(IndexOutOfBoundsException e){
					//如果发生了数组越界，说明该数据包还没有达到可解析一个完整帧头的长度，所以继续读取即可
					finished = false;
					break;
				}
			}
			case READ_DATA:{
				if(processor.readPayloadData(buffer)){
					finished = true;
					break;
				}
			}
			case RUNNING:break;
			}
			
		}finally{
			buffer.compact();
		}
		return finished;
	}
}
