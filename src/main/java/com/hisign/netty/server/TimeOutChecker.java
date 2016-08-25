package com.hisign.netty.server;

import java.util.concurrent.DelayQueue;

import com.hisign.bean.Request;
import com.hisign.hbve.protocol.HBVEMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeOutChecker implements Runnable {
	
	DelayQueue<HBVEMessage> timeOutQueue;
	boolean running;
	
	private NettyServer server;

    static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	public TimeOutChecker(NettyServer server, DelayQueue<HBVEMessage> queue){
		timeOutQueue = queue;
		running = true;
		this.server = server;
	}

	public void run() {
        logger.info("timeout checkouter start");
		while(running){
            try {
//                System.out.println("检查ing");
				HBVEMessage conn = timeOutQueue.take();
                timeOutProcess(conn);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
	}
	
	private void timeOutProcess(HBVEMessage conn){
		System.out.println("find timeout message");
		conn.setIsTimeOut();
	}
}
