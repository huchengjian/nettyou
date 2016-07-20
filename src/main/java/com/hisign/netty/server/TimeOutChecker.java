package com.hisign.netty.server;

import java.util.concurrent.DelayQueue;

public class TimeOutChecker implements Runnable {
	
	DelayQueue<Connection> timeOutQueue;
	boolean running;
	
	private NettyServer server;
	
	public TimeOutChecker(NettyServer server, DelayQueue<Connection> queue){
		timeOutQueue = queue;
		running = true;
		this.server = server;
	}

	public void run() {
		while(running){
            try {
                System.out.println("检查ing");
                Connection conn = timeOutQueue.take();
                
                timeOutProcess(conn);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	}
	
	private void timeOutProcess(Connection conn){
		conn.setTimeOut();
		System.out.println("超时" + conn.getMsg());
	}
}
