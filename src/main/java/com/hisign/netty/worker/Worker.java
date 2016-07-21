package com.hisign.netty.worker;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;
import com.hisign.netty.server.NettyServer;

public class Worker {

	public static AtomicInteger workerCount = new AtomicInteger(0);
	
	static private Logger logger = LoggerFactory.getLogger(NettyServer.class);

	public static void main(String[] args) throws InterruptedException {

		if (args != null && args.length >= 2) {
			try {
				WorkerRunnable.serverPort = Integer.parseInt(args[0]);
				WorkerRunnable.serverIp = args[1];
			} catch (NumberFormatException e) {
				System.out.println("port error. use integer value.");
			}
		}

//		Worker worker = new Worker();
		
		logger.info("worker start at " + WorkerRunnable.serverIp + ". Thread Count:" + SystemConstants.MaxWorker);
		for(int i = 0; i < SystemConstants.MaxWorker; i++){
			new Thread(new WorkerRunnable()).start();
		}
		Thread.sleep(2000);
	}

	/**
	 * not used
	 */
	public void run() {
		try {
			while (true) {
				if (workerCount.get() >= SystemConstants.MaxWorker) {
					//以后可改成通知机制，而非睡眠
					Thread.sleep(100);
					continue;
				}

				workerCount.getAndIncrement();
				System.out.println("新建任务,workerCount: " + workerCount.get());

				new Thread(new Runnable() {
					public void run() {
						NettyWorker nettyClient = new NettyWorker();
						try {
							nettyClient.connect("127.0.0.1", SystemConstants.NettyServerPort);
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
