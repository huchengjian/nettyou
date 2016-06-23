package com.hisign.netty.worker;

import com.hisign.constants.SystemConstants;

public class WorkerRunnable implements Runnable {

	public void run() {
		try {
			while (true) {

				NettyWorker nettyClient = new NettyWorker();
				nettyClient.connect("127.0.0.1", SystemConstants.NettyServerPort);
				System.out.println(Thread.currentThread()+ " 新任务");
				Thread.sleep(100);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
}
