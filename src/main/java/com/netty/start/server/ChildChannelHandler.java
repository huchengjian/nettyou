package com.netty.start.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * 客户端通道处理类.
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel e) throws Exception {
    	
    	System.out.println("initChannel");

        ChannelPipeline pipeline = e.pipeline();
        // 以("\n")为结尾分割的 解码器
        pipeline.addLast(new LineBasedFrameDecoder(1024));
        // 字符串解码 和 编码
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        //添加消息处理
        e.pipeline().addLast(new NettyServerHandler());

    }

}