package com.hisign.netty.worker;

import java.util.concurrent.atomic.AtomicInteger;

import com.hisign.constants.SystemConstants;

public class WorkerRunnable {

	public static AtomicInteger workerCount = new AtomicInteger(0);

	public static void main(String[] args) {
		
	}

	public void run() {
		try {
			while (true) {

				if (workerCount.get() >= SystemConstants.WorkerCount) {
					Thread.sleep(200);
					continue;
				}
				NettyWorker nettyClient = new NettyWorker();
				nettyClient.connect("127.0.0.1", 8099);
				workerCount.incrementAndGet();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
