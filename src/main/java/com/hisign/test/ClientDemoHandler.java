package com.hisign.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;

import java.net.SocketAddress;

/**
 * 客户端处理类.
 */
public class ClientDemoHandler extends ChannelInboundHandlerAdapter  {
	
	
	public String output = "----CLientDemo---";
	
	public ClientDemoHandler() {
	}

    static private Logger logger = LoggerFactory.getLogger(ClientDemoHandler.class);

//    /**
//     * 连接通道.
//     *
//     * @param ctx
//     * @param remoteAddress
//     * @param localAddress
//     * @param promise
//     * @throws Exception
//     */
//    @Override
//    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
//        logger.info(output + remoteAddress + "：连接通道");
//        super.connect(ctx, remoteAddress, localAddress, promise);
//    }

    /**
     * 活跃通道.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(output + ctx.channel().remoteAddress() + "：通道激活");
//        super.channelActive(ctx);
        
        ByteBuf firstMessage;
        firstMessage = Unpooled.buffer(1024);
        
        JSONObject jo = new JSONObject();
        jo.put(Message.MessageType, 1);
        jo.put(Message.DATA, "{\"verify1\": \"fdsafsa\", \"verify2\": \"123242\", \"type1\": 1, \"type2\": 2}");
        
        firstMessage.writeBytes(jo.toJSONString().getBytes());
        ctx.writeAndFlush(firstMessage);
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
//        super.channelInactive(ctx);
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
        
        System.out.println(output + "client channelRead..");
        
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        
        String body = new String(req, "UTF-8");
        System.out.println(output + "" + body);
        logger.info("Finish Task!");
        ctx.close();
    }

    /**
     * 接收完毕.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        super.channelReadComplete(ctx);
        System.out.println("ReadComplete");
    }

//    /**
//     * 关闭通道.
//     *
//     * @param ctx
//     * @param promise
//     * @throws Exception
//     */
//    @Override
//    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//        super.close(ctx, promise);
//    }

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
