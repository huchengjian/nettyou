package com.hisign.bean;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Request implements Delayed {
	
	private boolean isTimeOut = false;

	public byte[] para;
	
	public int message_type;
	
	private ChannelHandlerContext channelHandlerContext;

	public int compareTo(Delayed o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getDelay(TimeUnit unit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean getIsTimeOut(){
		return isTimeOut;
	}
	
	public void setIsTimeOut(){
		isTimeOut = true;
	}
	
	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}


	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}
	
}
