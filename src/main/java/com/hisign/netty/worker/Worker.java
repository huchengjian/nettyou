package com.hisign.netty.worker;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;
import com.hisign.netty.server.NettyServer;
import com.hisign.thid.THIDFaceSDK;

public class Worker {
	static private Logger logger = LoggerFactory.getLogger(NettyServer.class);
	
	static{
		int status = THIDFaceSDK.init(null, null, null);
		if (status < 0) {
			logger.info("\nTHIDFaceSDK init error, error code:" + status);
			System.exit(status);
		}
		logger.info("\nTHIDFaceSDK init success, code:" + status);
	}

	public static AtomicInteger workerCount = new AtomicInteger(0);
	
	
	static String LOCALHOST = SystemConstants.NettyServerAddr;
	static int LOCALPORT = 8091;

	public static void main(String[] args) throws InterruptedException {
		
//		List<String> ips = new ArrayList<String>();
		String allServers = LOCALHOST;
		int port = LOCALPORT;

		if (args != null && args.length >= 2) {
			try {
				port = Integer.parseInt(args[0]);
				allServers = args[1].trim();
				logger.info("allServers:"+allServers);

				if (args.length >= 3) {
					SystemConstants.MaxWorker = Integer.parseInt(args[2]);
				}
			} catch (NumberFormatException e) {
				logger.error("Integer parse error. use integer value.");
			}
		}

		logger.info("Worker start. Thread count: " + SystemConstants.MaxWorker);
		
		int nameT = 0;
		for(int i = 0; i < SystemConstants.MaxWorker; i++){
			for (String ip : allServers.split(",")) {
				logger.info("connect to server " + ip);
				new Thread(new WorkerRunnable(port, ip, String.valueOf(nameT++))).start();
			}
		}
		Thread.sleep(2000);
	}

	/**
	 * Not used anymore.
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
				logger.info("new task, workerCount: " + workerCount.get());

				new Thread(new Runnable() {
					public void run() {
						NettyWorker nettyClient = new NettyWorker("");
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
