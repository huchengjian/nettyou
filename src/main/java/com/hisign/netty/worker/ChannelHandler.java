package com.hisign.netty.worker;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * 通道处理类.
 */
public class ChannelHandler extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

//        ChannelPipeline pipeline = socketChannel.pipeline();
//        // 以("\n")为结尾分割的 解码器
//        pipeline.addLast(new LineBasedFrameDecoder(1024));
//        // 字符串解码 和 编码
//        pipeline.addLast(new StringDecoder());
//        pipeline.addLast(new StringEncoder());
    	socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024*10000));
        socketChannel.pipeline().addLast(new NettyWorkerClientHandler());

    }
}
