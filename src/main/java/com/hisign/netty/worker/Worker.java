package com.hisign.netty.worker;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.hisign.THIDFaceSDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;
import com.hisign.netty.server.NettyServer;

public class Worker {
	
	public static Executor exetractTemplatePool;
	
	static private Logger logger = LoggerFactory.getLogger(NettyServer.class);
	
//	static{
//		int status = THIDFaceSDK.Init(1, null, null, null, null);
//		if (status < 0) {
//			logger.info("THIDFaceSDK init error, error code:{}\n", status);
//			System.exit(status);
//		}
//		logger.info("THIDFaceSDK init success, code:{}\n", status);
//    }

	public static AtomicInteger workerCount = new AtomicInteger(0);
	
	static String LOCALHOST = SystemConstants.NettyServerAddr;
	static int LOCALPORT = 8091;

	public static void main(String[] args) throws InterruptedException {
        
//		List<String> ips = new ArrayList<String>();
		String allServers = LOCALHOST;
		int port = LOCALPORT;

		if (args != null && args.length >= 3) {
			try {
				allServers = args[0].trim();
				logger.info("allServers:"+allServers);
				port = Integer.parseInt(args[1]);
				SystemConstants.MaxWorker = Integer.parseInt(args[2]);
				SystemConstants.MAX_SDK_BATCH = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				logger.error("Integer parse error. use integer value.");
			}
		}
        
        logger.info("------------启动HBVE Worker服务, MAX_SDK_BATCH:{}-----------------\n", SystemConstants.MaxWorker);

		exetractTemplatePool = Executors.newFixedThreadPool(SystemConstants.MaxWorker);

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
			e.printStackTrace();
		}
	}
}
