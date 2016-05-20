package com.netty.start.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * 服务器处理类.
 */
public class NettyServerHandler extends ChannelHandlerAdapter {

    static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    //创建频道组
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 连接通道.
     *
     * @param ctx
     * @param remoteAddress
     * @param localAddress
     * @param promise
     * @throws Exception
     */
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        logger.info(remoteAddress + "：连接通道");
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    /**
     * 活跃通道.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：通道激活");
        super.channelActive(ctx);
        ctx.writeAndFlush("欢迎访问服务器\r\n");
        channels.add(ctx.channel());
    }

    /**
     * 非活跃通道.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：通道失效");
        super.channelInactive(ctx);
        channels.remove(ctx.channel());
    }

    /**
     * 接收消息.
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：" + msg);
        Channel currentChannel = ctx.channel();
        for (Channel channel : channels) {
            if (channel != currentChannel) {
                channel.writeAndFlush("[" + currentChannel.remoteAddress() + "]" + msg + "\n");
            }
        }
    }

    /**
     * 接收完毕.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    /**
     * 关闭通道.
     *
     * @param ctx
     * @param promise
     * @throws Exception
     */
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：关闭通道");
        super.close(ctx, promise);
    }

    /**
     * 异常处理.
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("异常信息：" + cause.getMessage());
    }
}
