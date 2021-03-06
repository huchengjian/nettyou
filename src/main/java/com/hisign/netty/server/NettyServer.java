package com.hisign.netty.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.hisign.constants.SystemConstants;
import com.hisign.decoder.MessageDecoder;
import com.hisign.decoder.ValidateDecoder;
import com.hisign.hbve.protocol.HBVEMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyServer {
	
	static String ServerVersion = "1.01";
	
	static private Logger logger = LoggerFactory.getLogger(NettyServer.class);
	
	public Map<String, HBVEMessage> consumingChannel;
//	public Queue<HBVEMessage> allClientQueue;
//	public Queue<HBVEMessage> waitingQueue;
	
	public Queue<HBVEMessage> allClientQueue;
	public Map<Float, Queue<HBVEMessage>> waitingQueue;
	
//	public Map<String, Long> surviveWorker;
	
	public static AtomicReference<Float> maxWorkerVersion = new AtomicReference<Float>(0f);  
	
	NettyServer(){
		consumingChannel = new ConcurrentHashMap<String, HBVEMessage>();
		
		allClientQueue = new ConcurrentLinkedQueue<HBVEMessage>();
		waitingQueue = new HashMap<Float, Queue<HBVEMessage>>();
		
//		surviveWorker = new HashMap<String, Long>();
//		allClientQueue = new ConcurrentLinkedQueue<HBVEMessage>();
//		waitingQueue = new ConcurrentLinkedQueue<HBVEMessage>();
	}
	
    public void bind(int port) throws Exception {

    	logger.info("-----------------------------------------------------------------------------------------");
    	logger.info("------------------------启动服务器, 端口:" + port + ", 服务器版本:" + ServerVersion+ "-------------------------------");
    	logger.info("-----------------------------------------------------------------------------------------");
    	
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(10);
        try {
            // 配置服务器的NIO线程组
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler(this));

            // 绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
    	
    	NettyServer server;
    	
    	public ChildChannelHandler(NettyServer server) {
    		this.server = server;
		}
    	
        @Override
        protected void initChannel(SocketChannel sc) throws Exception {
            logger.info("server initChannel..");
            sc.pipeline().addLast(new ValidateDecoder());
//            sc.pipeline().addLast(new LineBasedFrameDecoder(1024*1000000));
            sc.pipeline().addLast("lengthDecoder", 
            		new LengthFieldBasedFrameDecoder(20*1024*1024,0,4,0,4));
//            sc.pipeline().addLast(new StringDecoder());
            sc.pipeline().addLast(new MessageDecoder());
            sc.pipeline().addLast(new NettyServerHandler(server));
        }
    }
    
    public void addWatiWorker(HBVEMessage worker){
    	float verion = worker.header.workerSDKVersion;
    	if (!waitingQueue.containsKey(verion)) {
    		waitingQueue.put(verion, new ConcurrentLinkedQueue<HBVEMessage>());
		}
    	waitingQueue.get(verion).add(worker);
    }
    
//    public void addClient(HBVEMessage client){
//    	String verion = client.header.workerSDKVersion;
//    	if (!allClientQueue.containsKey(verion)) {
//    		allClientQueue.put(verion, new ConcurrentLinkedQueue<HBVEMessage>());
//		}
//    	allClientQueue.get(verion).add(client);
//    }

    public static void main(String[] args) throws Exception {

        logger.info(System.getProperty("user.dir"));
        int port = SystemConstants.NettyServerPort;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                logger.info("port error. use integer value.");
            }
        }
        NettyServer server = new NettyServer();
        
        ShutdownDaemonHook shutdownHook = new ShutdownDaemonHook(server);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        server.bind(port);
    }

    public static class ShutdownDaemonHook extends Thread {

        NettyServer server;
        ShutdownDaemonHook(NettyServer s){
            server = s;
        }

        /**
         * 循环并使用hook关闭所有后台线程
         *
         * @see java.lang.Thread#run()
         */
        @Override
		public void run() {
			logger.info("Running shutdown sync");

			Queue<HBVEMessage> queue = server.allClientQueue;

			Iterator<HBVEMessage> iterator = queue.iterator();
			while (iterator.hasNext()) {
				HBVEMessage message = iterator.next();
				logger.info("close conn: "
						+ message.ctx.channel().remoteAddress() + ". Id: "
						+ message.header.connId);
				message.ctx.channel().close();
			}
		}
	}
}

