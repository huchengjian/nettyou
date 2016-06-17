package com.hisign.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageProcess {
	
	
	public static int defaultLength = 1000;
	
	public static ByteBuf writeStringMessage(String mess, String tontent) {
		byte[] pa = mess.getBytes();
    	ByteBuf firstMessage = Unpooled.buffer(defaultLength);
		return firstMessage;

	}
	
}
