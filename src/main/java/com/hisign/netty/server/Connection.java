package com.hisign.netty.server;

import io.netty.channel.ChannelHandlerContext;

public class Connection {
	
	private ChannelHandlerContext channelHandlerContext;
	private String msg;
	private long timestamp;
	
	Connection(ChannelHandlerContext channelHandlerContext, String msg){
		this.channelHandlerContext = channelHandlerContext;
		this.msg = msg;
		timestamp = System.currentTimeMillis();
	}
	
	
	public int hashCode(){
		int result = 0;
		String hashStr = String.valueOf(timestamp) + msg;
		result = hashStr.hashCode();
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
