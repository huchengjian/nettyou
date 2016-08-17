package com.hisign.netty.worker;

import com.hisign.hbve.protocol.HBVEBinaryProtocol;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.hbve.protocol.HBVEProcesser;
import com.hisign.netty.worker.handler.ComputeSimilarityHandler;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;
import com.hisign.exception.HisignSDKException;

import java.io.IOException;

/**
 * 客户端处理类.
 */
public class NettyWorkerClientHandler extends ChannelInboundHandlerAdapter  {
	
	boolean isFirstReq = true;
	
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

        fetchJobFromMaster(ctx);
        
        isFirstReq = false;
    }

    public void fetchJobFromMaster(ChannelHandlerContext ctx){

        logger.info("fetchJobFromMaster");

        ByteBuf request;
        request = Unpooled.buffer(20);

//        RequestService.addValidateFields(jo);

        if (isFirstReq) {
            request.writeBytes(getHeader());
        }
        
        request.writeInt(1);//length
        request.writeByte(HBVEMessageType.WORKER_FLAG);
        
        ctx.writeAndFlush(request);
    }
    
    /**
     * 非活跃通道.
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
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	

    	logger.info(ctx.channel().remoteAddress() + "：" + msg);
		logger.info("worker channelRead..");
		
		try {
			HBVEMessage task = (HBVEMessage) msg;
			task.ctx = ctx;
			doTask(task);
		} catch (HisignSDKException e) {
			logger.error("doTask HisignSDKException");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("doTask IOException");
			e.printStackTrace();
		}
		finally{
			fetchJobFromMaster(ctx);
		}
	}

    public void doTask(HBVEMessage task) throws HisignSDKException, IOException {
    	
    	if ( HBVEMessageType.getMessageType(task.header.messageType).equals(HBVEMessageType.MessageType.Error) ) {
			//服务器错误消息handle
		}
    	else if (HBVEMessageType.isWorkerMess(task.header.messageType)) {
			if (HBVEMessageType.getClientMessageType(task.header.messageType)
					.equals(HBVEMessageType.ClientMessageType.Similarity)) {
				ComputeSimilarityHandler computeSimilarityHandler = new ComputeSimilarityHandler();
				float score = (float) 0.98;
//				float score = computeSimilarityHandler.run(task.data);
				
				sendResult(
						task.header.messageType, 
						task.header.uuid.getBytes(),
						SystemUtil.float2byte(score),
						task.ctx
						);
			}
			// Todo add new task type, 可以通过反射拿到处理handler
		}
    }
    
    public void sendResult(byte type, byte[] uuid, byte[] data, ChannelHandlerContext ctx){
    	
    	int len = 1 + 32 + 4;
    	ByteBuf byteBuf = Unpooled.buffer(len);

    	byteBuf.writeByte(type);
    	byteBuf.writeBytes(uuid);
    	byteBuf.writeBytes(data);
//
//        ctx.writeAndFlush(byteBuf);
        
        HBVEBinaryProtocol.writeChannel(ctx, byteBuf.array());
    }
    
    /**
     * 异常处理.
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("异常信息：" + cause.getMessage());
    }

    private byte[] getHeader() {
        return (SystemConstants.MAGIC + SystemConstants.CURRENT_VERSION).getBytes();
    }
}
