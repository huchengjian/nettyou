package com.hisign.netty.worker;

import com.alibaba.fastjson.JSON;
import com.hisign.netty.server.Connection;
import com.hisign.netty.service.RequestService;
import com.hisign.util.SystemUtil;

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
import com.hisign.constants.SystemConstants;

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;

/**
 * 客户端处理类.
 */
public class NettyWorkerClientHandler extends ChannelInboundHandlerAdapter  {
	
	
	public NettyWorkerClientHandler() {
	}

    static private Logger logger = LoggerFactory.getLogger(NettyWorkerClientHandler.class);

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
//        logger.info(remoteAddress + "：连接通道");
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
        logger.info(ctx.channel().remoteAddress() + "：通道激活");
//        super.channelActive(ctx);
        
        ByteBuf firstMessage;
        firstMessage = Unpooled.buffer(1024);
        
        JSONObject jo = new JSONObject();
        jo.put(Message.MessageType, 2);
        RequestService.addValidateFields(jo);
        
        firstMessage.writeBytes(SystemUtil.addNewLine(jo.toJSONString()).getBytes());
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
        
        logger.info("worker channelRead..");
        
        ByteBuf buf = (ByteBuf) msg;
//        System.out.println(buf.readableBytes());
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        
        String body = new String(req, "UTF-8");
        JSONObject jo = JSON.parseObject(body);
        
        int status = jo.getInteger(Message.Status);
        if (status < 0) {
        	logger.info("Worker 收到消息错误:"+body);
        	ctx.close();
        	return;
		}
        
        JSONObject connData = JSONObject.parseObject(jo.getString(Message.DATA));
        float score = (float) 0.98;
//        score = compute(connData);

        int connId = jo.getInteger(Message.ConnId);
        JSONObject result = new JSONObject();
        result.put(Message.MessageType, 3);
        result.put(Message.ConnId, connId);
        result.put(Message.Score, score);
        result.put(Message.Status, 0);
        RequestService.addValidateFields(result);

        byte[] data = SystemUtil.addNewLine(result.toJSONString()).getBytes();
        ByteBuf bb = Unpooled.buffer(1024);
        bb.writeBytes(data);
        ctx.writeAndFlush(bb);
    }
    
    public float compute(JSONObject data) throws UnsupportedEncodingException{
    	
    	logger.info("compute similarity!");
    	
    	String  verify1 = data.getString(Message.Verify1);
    	String  verify2 = data.getString(Message.Verify2);
    	int  type1 = data.getIntValue(Message.Type1);
    	int  type2 = data.getIntValue(Message.Type2);
    	
    	byte[] temp1= null, temp2 = null;
    	if (type1 == 1) {
    		//图片數據
			temp1 = HisignBVESDK.getTemplateByImageByteArray(verify1.getBytes(SystemConstants.ENCODED));
        }
    	if (type2 == 1) {
    		//图片數據
			temp2 = HisignBVESDK.getTemplateByImageByteArray(verify2.getBytes(SystemConstants.ENCODED));
        }
    	
    	return HisignBVESDK.compareFromTwoTemplate(temp1, temp2);
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
//    	System.out.println("close current worker channel");
//    	Worker.workerCount.getAndDecrement();
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
