package com.hisign.decoder;

import java.util.List;

import com.hisign.hbve.protocol.HBVEHeader;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder{

    /**
     * 取出头部和数据字段, client头部为1+4字节, worker头部为1+32字节, 剩余为data数据
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		
        HBVEHeader header = null;

		try {
			if (in.readableBytes() <= 1) {
				return;
			}
			byte type = in.readByte();

            if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Fetch)){
                header = new HBVEHeader(type, "");
            }
            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Result)
                    ||
                    HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Error_Result)
                    ){
                byte[] uuid = new byte[32];
                in.readBytes(uuid);
                header = new HBVEHeader(type, new String(uuid));
            }

            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Client)){
                int id = in.readInt();
                header = new HBVEHeader(type, id);
            }

            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Error)){
                System.out.println("error message");
                byte[] data = new byte[in.readableBytes()];
                System.out.println(in.readBytes(data));
            }

			byte[] data = new byte[in.readableBytes()];
	        in.readBytes(data);
	        HBVEMessage hm = new HBVEMessage(header, data);
	        out.add(hm);

//            hm.print();
		} catch (Exception e) {
			System.out.println("ERROR: Message Decoder Error!");
            //Todo 返回结果
			e.printStackTrace();
		}
	}
}