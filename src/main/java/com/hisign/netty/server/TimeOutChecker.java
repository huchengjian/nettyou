package com.hisign.netty.server;

import java.util.concurrent.DelayQueue;

import com.hisign.bean.ClientRequest;
import com.hisign.bean.Request;

public class TimeOutChecker implements Runnable {
	
	DelayQueue<Request> timeOutQueue;
	boolean running;
	
	private NettyServer server;
	
	public TimeOutChecker(NettyServer server, DelayQueue<Request> queue){
		timeOutQueue = queue;
		running = true;
		this.server = server;
	}

	public void run() {
		System.out.println("timeout checkouter start");
		while(running){
            try {
//                System.out.println("检查ing");
            	Request conn = timeOutQueue.take();
                
                timeOutProcess(conn);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	}
	
	private void timeOutProcess(Request conn){
//		System.out.println("超时" + conn.getMsg());
		conn.setIsTimeOut();
	}
}
