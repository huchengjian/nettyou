package com.hisign.hbve.protocol;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;

public class HBVEMessage implements Delayed{
	
	  public HBVEHeader header;
	  public byte[] data;
	  
	  public ChannelHandlerContext ctx = null;
	  
	  public long timestamp = System.currentTimeMillis();
	  
	  public boolean isTimeOut = false;
	  
	  public HBVEMessage(HBVEHeader h, byte[] d){
		  header = h;
		  data = d;
	  }

    public void print() {
        System.out.println("type + dataLen:" + (int)header.messageType + " " + data.length);
    }

    public int compareTo(Delayed o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getDelay(TimeUnit unit) {
		// TODO Auto-generated method stub
		return 0;
	}
}
