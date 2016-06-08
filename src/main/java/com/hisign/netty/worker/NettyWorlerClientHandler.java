package com.hisign.netty.worker;

import com.alibaba.fastjson.JSON;
import com.hisign.netty.server.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;

import java.net.SocketAddress;

/**
 * 客户端处理类.
 */
public class NettyWorlerClientHandler extends ChannelHandlerAdapter {
	
	private ByteBuf firstMessage;
	
	public NettyWorlerClientHandler() {
		byte[] req = "Hello hugo".getBytes();
        firstMessage = Unpooled.buffer(req.length);
        firstMessage.writeBytes(req);
	}

    static private Logger logger = LoggerFactory.getLogger(NettyWorlerClientHandler.class);

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
//        super.channelActive(ctx);
        
        ByteBuf firstMessage;
        firstMessage = Unpooled.buffer(1024);
        
        JSONObject jo = new JSONObject();
        jo.put(Message.MessageType, 2);
        jo.put(Message.DATA, "222222");
        
        firstMessage.writeBytes(jo.toJSONString().getBytes());
        ctx.writeAndFlush(firstMessage);
    }
    
    private void jobRequest() {
    	
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
        Worker.workerCount.getAndDecrement();
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
        
        System.out.println("worker channelRead..");
        
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        
        String body = new String(req, "UTF-8");
        JSONObject jo = JSON.parseObject(body);
        
        int status = (int)jo.get(Message.Status);
        if (status < 0) {
        	logger.info("Worker 收到消息错误");
        	ctx.close();
		}
        
        JSONObject conn = (JSONObject) jo.get(Message.DATA);
        int connId = (int) jo.get(Message.ConnId);

        System.out.println("" + body);

        compute();

        JSONObject result = new JSONObject();
        result.put(Message.MessageType, 3);
        result.put(Message.DATA, conn);
        result.put(Message.ConnId, connId);

        byte[] data = result.toJSONString().getBytes();
        ByteBuf bb = Unpooled.buffer(1024);
        bb.writeBytes(data);
        ctx.writeAndFlush(bb);
    }

    public void compute(){
        int sum_1 = 0;
        for (int i = 0; i < 10000; i++) {
            sum_1 += i;
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
//        super.channelReadComplete(ctx);
        System.out.println("ReadComplete");
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
    	System.out.println("close current worker channel");
    	Worker.workerCount.getAndDecrement();
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
