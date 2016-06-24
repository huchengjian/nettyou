package com.hisign.netty.server;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hisign.constants.SystemConstants;
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
import io.netty.handler.codec.LineBasedFrameDecoder;

public class NettyServer {
	
	static private Logger logger = LoggerFactory.getLogger(NettyServer.class);
	
	public Map<String, Connection> consumingChannel;
	public Queue<Connection> allConnChannelQueue;
	
	NettyServer(){
		consumingChannel = new ConcurrentHashMap<String, Connection>();
		allConnChannelQueue = new ConcurrentLinkedQueue<Connection>();
	}
	
    public void bind(int port) throws Exception {

    	logger.info("----------------启动服务器----------------");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 配置服务器的NIO线程租
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
            System.out.println("server initChannel..");
            sc.pipeline().addLast(new LineBasedFrameDecoder(1024*10000));
//            sc.pipeline().addLast(new StringDecoder());
            sc.pipeline().addLast(new NettyServerHandler(server));
        }
    }

    public static void main(String[] args) throws Exception {


        int port = SystemConstants.NettyServerPort;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {

            }
        }
        new NettyServer().bind(port);
    }
}