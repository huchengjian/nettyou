package com.hisign.decoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.hbve.protocol.HBVEHeader;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.hbve.protocol.HBVEProtocol;
import com.hisign.netty.server.Metrics;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder{
	
	static private Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

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
		
		recordMetrics(ctx);
		
        HBVEHeader header = null;

		try {
			if (in.readableBytes() <= 0) {
				return;
			}
			byte type = in.readByte();

            if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Fetch)){
            	header = new HBVEHeader(type);
            	
                byte[] workerSDKVersion = new byte[HBVEProtocol.Header_SDKVERSION_LEN];
                in.readBytes(workerSDKVersion);
                header.workerSDKVersion = Float.intBitsToFloat(SystemUtil.fourByteArrayToInt(workerSDKVersion));
            }
            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Result)
                    ||
                    HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Error_Result)
                    ){
                byte[] uuid = new byte[HBVEProtocol.Header_UUID_LEN];
                in.readBytes(uuid);
                header = new HBVEHeader(type, new String(uuid));
            }

            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Client)){
            	
//            	byte[] workerSDKVersion = new byte[3];
//              in.readBytes(workerSDKVersion);
                int id = in.readInt();
                header = new HBVEHeader(type, id);
            }

            else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Error)){
            	logger.info("error message");
                byte[] data = new byte[in.readableBytes()];
            }

			byte[] data = new byte[in.readableBytes()];
	        in.readBytes(data);
	        HBVEMessage hm = new HBVEMessage(header, data);
	        hm.timeout = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);
	        
	        out.add(hm);
//          hm.print();
	        logger.info("Receive Message." +
					" messageType:" + hm.header.messageType +
					" uuid:" + hm.header.connId +
					" data_len:" + data.length
					);
		} catch (Exception e) {
			logger.info("ERROR: Message Decoder Error!");
            //Todo 返回结果
			e.printStackTrace();
		}
	}
	
	void recordMetrics(ChannelHandlerContext ctx){
		String addr = ctx.channel().remoteAddress().toString();
		Metrics.lastConnTimeMap.put(addr, System.currentTimeMillis());
	}
	
}
