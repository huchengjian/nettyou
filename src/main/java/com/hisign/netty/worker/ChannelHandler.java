package com.hisign.netty.worker;

import com.hisign.decoder.MessageDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 通道处理类.
 */
public class ChannelHandler extends ChannelInitializer<SocketChannel> {
	
	public String tName;
	
	public ChannelHandler(String name){
		tName = name;
	}

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

//        ChannelPipeline pipeline = socketChannel.pipeline();
//        // 以("\n")为结尾分割的 解码器
//        // 字符串解码 和 编码
//    	socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024*1000000));
//    	socketChannel.pipeline().addLast(new ValidateDecoder());
        socketChannel.pipeline().addLast("lengthDecoder",
                new LengthFieldBasedFrameDecoder(200*1024*1024,0,4,0,4));
        socketChannel.pipeline().addLast(new MessageDecoder());
        socketChannel.pipeline().addLast(new NettyWorkerClientHandler(tName));

    }
}
