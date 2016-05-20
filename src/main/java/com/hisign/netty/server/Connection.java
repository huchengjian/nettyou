package com.hisign.netty.server;

import io.netty.channel.ChannelHandlerContext;

public class Connection {
	
	public ChannelHandlerContext channelHandlerContext;
	public String msg;
	
	Connection(ChannelHandlerContext channelHandlerContext, String msg){
		this.channelHandlerContext = channelHandlerContext;
		this.msg = msg;
	}
	
	
	public int hashCode(){
		int result = 0;
		
		result = msg.hashCode();
		
		return result;
	}
}
