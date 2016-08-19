package com.hisign.decoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ValidateDecoder extends ByteToMessageDecoder {
	
	int i = 1;
	
	boolean isValidated = false;
	
	private enum State {
        INIT,
        OVER,
        FINISHED,
        CORRUPTED
    }
	
	static private Logger log = LoggerFactory.getLogger(ValidateDecoder.class);
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {

		ByteBuf byteBuf = Unpooled.buffer(1024);
		
		//todos 判断可读字节是否够
		
		if(!isValidated){
			byte[] head = new byte[4];
			in.readBytes(head);
			if (! new String(head, "utf-8").equals(SystemConstants.MAGIC)) {
				log.error("Receive Error HEADER:" + new String(head, "utf-8"));
				in.clear();
				ctx.channel().close();
				return;
			}
			
			byte[] version = new byte[3];
			in.readBytes(version);
			if (!new String(version, "utf-8").equals(SystemConstants.CURRENT_VERSION)) {
				log.error("Receive Error Version:" + new String(version, "utf-8"));
				in.clear();
				ctx.channel().close();
				return;
			}
			log.info("new conn. validate header pass.");
		}
		isValidated = true;
		
		
        byte[] req = new byte[in.readableBytes()];
        in.readBytes(req);

    	byteBuf.writeBytes(req);
    	out.add(byteBuf);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelInactive();
	}
}