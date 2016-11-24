package com.hisign.hbve.protocol;

import com.hisign.constants.SystemConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public abstract class HBVEProcesser {
	
	public abstract void process();
	
	/**
	 * write data into channel
	 * @param ctx
	 * @param data header+para
	 */
//	public static void writeChannel(ChannelHandlerContext ctx, byte[] data){
//    	ByteBuf byteBuf = Unpooled.buffer(1024);
//    	byteBuf.writeBytes(SystemConstants.MAGIC_BYTES);
//    	byteBuf.writeBytes(SystemConstants.CURRENT_VERSION_BYTES);
//    	
//    	byteBuf.writeInt(data.length);
//		byteBuf.writeBytes(data);
//    	
//    	ctx.writeAndFlush(byteBuf);
//    }

}
