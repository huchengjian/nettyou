package com.hisign.netty.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkerRunnable implements Runnable {
	
	public String name;

	public int serverPort;
	public String serverIp;
	
	public static Logger logger = LoggerFactory.getLogger(WorkerRunnable.class);
	
	WorkerRunnable(int port, String ip, String name){
		serverPort = port;
		serverIp = ip;
		this.name = name;
	}
	
	public void run() {
		while (true) {
			try {
				logger.info("Thread {} Connect {}:{} \n", name, serverIp, serverPort);
				NettyWorker worker = new NettyWorker(name);
				worker.connect(serverIp, serverPort);
			} catch (Exception e) {
//				e.printStackTrace();
				logger.info("Thread {} Connect {}:{} error, sleep 8s and try again.\n", Thread.currentThread().getName(), serverIp, serverPort);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
