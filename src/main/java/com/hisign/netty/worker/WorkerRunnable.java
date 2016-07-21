package com.hisign.netty.worker;

import com.hisign.constants.SystemConstants;

public class WorkerRunnable implements Runnable {

	public static int serverPort;
	public static String serverIp;

	/**
	 * 初始化默认的ip、端口
	 */
	static{
		serverPort = SystemConstants.NettyServerPort;
		serverIp = SystemConstants.NettyServerAddr;
	}
	
	public void run() {
		while (true) {
			try {
				NettyWorker nettyClient = new NettyWorker();
				nettyClient.connect(serverIp, serverPort);
			} catch (Exception e) {
				e.printStackTrace();
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				System.out.println("connection error, sleep 3s and try again.");
			}
		}
	}
}
