package com.hisign.netty.worker;

import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.netty.worker.handler.ComputeSimilarityHandler;
import com.hisign.netty.worker.handler.ExtractTemplateHandler;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.constants.SystemConstants;
import com.hisign.exception.HisignSDKException;
import com.hisign.exception.ParseParaException;
import com.hisign.exception.WorkerException;

import java.io.IOException;

/**
 * 客户端处理类.
 */
public class NettyWorkerClientHandler extends ChannelInboundHandlerAdapter  {
	
	boolean isFirstReq = true;
	
	public NettyWorkerClientHandler() {
	}
	
	byte[] currUUID = {};

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
            request.writeBytes(SystemConstants.MAGIC_BYTES);
            request.writeBytes(SystemConstants.CURRENT_VERSION_BYTES);
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
    	
		
		HBVEMessage task = (HBVEMessage) msg;
		byte[] uuid = task.header.uuid.getBytes();
		currUUID = uuid;
		task.ctx = ctx;
		
		logger.info(ctx.channel().remoteAddress() + "：" + msg);
		logger.info("worker channelRead.." + " messageType:" + task.header.messageType + " uuid:" + task.header.uuid + " para_len:" + task.data.length);
		
		try {
			byte[] result = doTask(task);
			
			sendResult(
					task.header.messageType, 
					uuid,
					result,
					task.ctx
					);
			logger.info("Finish task." + " messageType:" + task.header.messageType + " uuid:" + task.header.uuid + " result:" + new String(result));
			
		} catch (HisignSDKException e) {
			logger.error("doTask HisignSDKException");
			e.printStackTrace();
			sendResult(HBVEMessageType.getErrorCode(e.errorId), uuid, e.getMessage().getBytes(), ctx);
			
		} catch (IOException e) {
			logger.error("doTask IOException");
			e.printStackTrace();
		} catch (ParseParaException e) {
			logger.error("doTask ParseParaException");
			e.printStackTrace();
			sendResult(HBVEMessageType.getErrorCode(e.errorId), uuid, e.getMessage().getBytes(), ctx);
		} catch (WorkerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (Exception e) {
            e.printStackTrace();
            logger.info("Error");
        }
		finally{
			fetchJobFromMaster(ctx);
		}
	}

    public byte[] doTask(HBVEMessage task) throws HisignSDKException, IOException, ParseParaException, WorkerException {
    	
    	byte[] result = null;
    	
    	if ( HBVEMessageType.getMessageType(task.header.messageType).equals(HBVEMessageType.MessageType.Error) ) {
			throw new WorkerException();
		}
    	else if (HBVEMessageType.isWorkerMess(task.header.messageType)) {
    		
    		//计算相似度接口
			if (HBVEMessageType.getClientMessageType(task.header.messageType)
					.equals(HBVEMessageType.ClientMessageType.Similarity)) {
				
				ComputeSimilarityHandler computeSimilarityHandler = new ComputeSimilarityHandler();
				result = SystemUtil.float2byte((float) 0.98);
				result = computeSimilarityHandler.run(task.data);
				return result;
			}
			//取模板接口
			if (HBVEMessageType.getClientMessageType(task.header.messageType)
					.equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
				
				ExtractTemplateHandler extractTemplateHandler = new ExtractTemplateHandler();
				result = "huchengjian".getBytes();
				result = extractTemplateHandler.run(task.data);
				return result;
			}
			// Todo add new task type, 可以通过反射拿到处理handler
		}
    	return result;
    }
    
    public void sendResult(byte type, byte[] uuid, byte[] data, ChannelHandlerContext ctx){
    	
    	int messageLen = 1 + uuid.length + data.length; //数据包大小。 type + uuid + data
    	ByteBuf byteBuf = Unpooled.buffer(messageLen + 4);
    	
    	byteBuf.writeInt(messageLen);
    	
    	byteBuf.writeByte(type);
    	byteBuf.writeBytes(uuid);
    	byteBuf.writeBytes(data);

    	ctx.writeAndFlush(byteBuf);
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
        sendResult((byte)0xC1, currUUID, cause.getMessage().getBytes(), ctx);
    }

    private byte[] getHeader() {
        return (SystemConstants.MAGIC + SystemConstants.CURRENT_VERSION).getBytes();
    }
}
