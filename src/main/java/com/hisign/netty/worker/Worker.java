package com.hisign.netty.worker;

import java.util.concurrent.atomic.AtomicInteger;

import com.hisign.constants.SystemConstants;

public class Worker {

	public static AtomicInteger workerCount = new AtomicInteger(0);

	public static void main(String[] args) {
		Worker worker = new Worker();
		worker.run();
	}

	public void run() {
		try {
			while (true) {
				if (workerCount.get() >= SystemConstants.MaxWorker) {
					//以后可改成通知机制，而非睡眠
					Thread.sleep(20);
					continue;
				}

				workerCount.getAndIncrement();
				System.out.println("新建任务,workerCount: " + workerCount.get());

				new Thread(new Runnable() {
					@Override
					public void run() {
						NettyWorker nettyClient = new NettyWorker();
						try {
							nettyClient.connect("127.0.0.1", 8099);
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
