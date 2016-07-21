package com.hisign.netty.worker;

import com.hisign.constants.SystemConstants;

public class WorkerRunnable implements Runnable {

	public static int serverPort;
	public static String serverIp;

	static{
		serverPort = SystemConstants.NettyServerPort;
		serverIp = SystemConstants.NettyServerAddr;
	}

	public void run() {
		while (true) {
			try {
				NettyWorker nettyClient = new NettyWorker();
				nettyClient.connect(serverIp, serverPort);
				Thread.sleep(100);
			} catch (Exception e) {
//				e.printStackTrace();
				System.out.println("connection error");
			}
		}
	}
}
