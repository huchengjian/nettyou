package com.hisign.test;

import com.hisign.constants.SystemConstants;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 客户端类.
 */
public class ClientDemo {

    public void connect(String host, int port) throws Exception {

        //创建事件循环组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            //创建引导程序
            Bootstrap b = new Bootstrap();
            //设置消息循环
            b.group(group);
            //设置通道
            b.channel(NioSocketChannel.class);
            //配置通道参数：tcp不延迟
            b.option(ChannelOption.TCP_NODELAY, true);
            //设置通道处理
            b.handler(new ClentDemoChannelHandler());
            //发起异步链接，等待输入参数
            
            ChannelFuture f = b.connect(host, port).sync();
            // 等待客户端链路关闭
            f.channel().closeFuture().sync();
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            while (true) {
//                channel.writeAndFlush(in.readLine() + "\r\n");
//            }
        } finally {
            //关闭
            group.shutdownGracefully();
        }

    }
    
    public static void main(String[] args) {
//        ClientDemo nettyClient = new ClientDemo();

        int count = 0;

        try {
            while(true){
                for (int i = 0; i<10; i++){
                    new Thread(new Runnable(){
                        public void run() {
                            ClientDemo nettyClient = new ClientDemo();
                            try {
                                nettyClient.connect("127.0.0.1", SystemConstants.NettyServerPort);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                count ++;
                if (count >= 1){
                    break;
                }
            }

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
//    public static void main(String[] args) throws Exception {
//    	ClientDemo nettyClient = new ClientDemo();
//    	nettyClient.connect("127.0.0.1", SystemConstants.NettyServerPort);
//	}

}
