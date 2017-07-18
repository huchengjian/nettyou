package com.hisign.netty.server;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;
import com.hisign.constants.ValueConstant;

import io.netty.channel.ChannelHandlerContext;

/**
 * client Connect
 * @author hugo
 *
 */
public class Connection implements Delayed {
	
	private String connId;
	private int messType;
	
	private ChannelHandlerContext channelHandlerContext;
	private String msg;
	private long timestamp; //connect time
	
	private long endTime; //timeout timestamp
	private int aliveTime;
	
	private boolean isTimeOut;
	
	Connection(ChannelHandlerContext channelHandlerContext, String msg){
		this.channelHandlerContext = channelHandlerContext;
		this.msg = msg;
		timestamp = System.currentTimeMillis();
		isTimeOut = false;
	}
	
	public String getConnId() {
		String message = this.getMsg();
    	JSONObject para = JSON.parseObject(message);
    	String connId = para.getString(Message.ConnId);
    	return connId;
	}
	
	public void parseAndSetClientPara() {
		String message = this.getMsg();
    	JSONObject para = JSON.parseObject(message);
    	
    	if (para.containsKey(Message.ConnId)) {
    		connId = para.getString(Message.ConnId);
		}
    	if (para.containsKey(Message.MessageType)) {
    		messType = para.getIntValue(Message.MessageType);
		}
    	if (para.containsKey(Message.AliveTime)) {
    		aliveTime = para.getIntValue(Message.AliveTime);
    		endTime = System.currentTimeMillis() + aliveTime;
		}
    	else if ( !para.containsKey(Message.AliveTime) ){
    		endTime = System.currentTimeMillis() + ValueConstant.DefaultClientTimeOut;
    	}
	}
	
	public void setIsTimeOut(){
		isTimeOut = true;
	}
	
	public boolean getIsTimeOut(){
		return isTimeOut;
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
	
	public void setEndTime(long time) {
		this.endTime = time;
	}

	public int compareTo(Delayed o) {
		return 0;
	}


	public long getDelay(TimeUnit unit) {
		return endTime - System.currentTimeMillis();
	}
}
