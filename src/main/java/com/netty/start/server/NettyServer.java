package com.netty.start.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 服务器类.
 */
public class NettyServer {

    public void start(int port) throws Exception {

        //创建接收者的事件循环组
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        //创建访问者的事件循环组
        EventLoopGroup childGroup = new NioEventLoopGroup();

        try {
            //创建服务器引导程序
            ServerBootstrap b = new ServerBootstrap();
            //设置消息循环
            b.group(parentGroup, childGroup);
            //设置通道
            b.channel(NioServerSocketChannel.class);
            //配置通道参数：连接队列的连接数
            b.option(ChannelOption.SO_BACKLOG, 1024);
            //设置客户端请求的处理操作
            b.childHandler(new ChildChannelHandler());
            //绑定端口，并获取通道io操作的结果
            ChannelFuture f = b.bind(port).sync();
            //等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            //关闭接收器事件循环
            parentGroup.shutdownGracefully();
            //关闭访问者的事件循环
            childGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) {
		System.out.println("hha");
		NettyServer server = new NettyServer();
		
		try {
			server.start(8090);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}