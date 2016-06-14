package com.hisign.netty.server;

import io.netty.channel.ChannelHandlerContext;

public class Connection {
	
	private ChannelHandlerContext channelHandlerContext;
	private String msg;
	
	Connection(ChannelHandlerContext channelHandlerContext, String msg){
		this.channelHandlerContext = channelHandlerContext;
		this.msg = msg;
	}
	
	
	public int hashCode(){
		int result = 0;
		
		result = msg.hashCode();
		
		return result;
	}


	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}


	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}


	public String getMsg() {
		return msg;
	}


	public void setMsg(String msg) {
		this.msg = msg;
	}
}
