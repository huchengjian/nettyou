package com.hisign.netty.worker;

import java.io.IOException;

import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.netty.worker.SDKResult.State;
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
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;

/**
 * 客户端处理类.
 */
public class NettyWorkerClientHandler extends ChannelInboundHandlerAdapter  {
	
	public String tName;
	
	boolean isFirstReq = true;
	
	public NettyWorkerClientHandler(String name){
		tName = name;
	}
	
	byte[] currUUID = {};
	int currType = 0;

    static private Logger logger = LoggerFactory.getLogger(NettyWorkerClientHandler.class);

    /**
     * 活跃通道.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：通道激活. " + "fetchJobFromMaster");
//        super.channelActive(ctx);

        fetchJobFromMaster(ctx);
        isFirstReq = false;
    }

    public void fetchJobFromMaster(ChannelHandlerContext ctx){
    	
    	logger.info(tName + " FetchJobFromMaster");

        ByteBuf request;
        request = Unpooled.buffer(20);

//        RequestService.addValidateFields(jo);

		//校验头部
        if (isFirstReq) {
            request.writeBytes(SystemConstants.MAGIC_BYTES);
            request.writeBytes(SystemConstants.CURRENT_VERSION_BYTES);
        }
        
        byte[] sdkVersionBytes = SystemUtil.int2byte(Float.floatToIntBits(8.3f));
        
        request.writeInt(1 + sdkVersionBytes.length);//length
        request.writeByte(HBVEMessageType.WORKER_FLAG);
        request.writeBytes(sdkVersionBytes);
        
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
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
    	
		HBVEMessage task = (HBVEMessage) msg;
		byte[] uuid = task.header.uuid.getBytes();
		currUUID = uuid;
		currType = task.header.messageType;
		task.ctx = ctx;
		
		
		logger.info(ctx.channel().remoteAddress() + "：" + msg);
		logger.info(tName + " thread. worker channelRead.." + " messageType:" + 
				task.header.messageType + " uuid:" + task.header.uuid + " para_len:" + task.data.length);
		
		SDKResult result = new SDKResult();
		try{
			result = doTask(task);
		
			logger.info(tName + " thread. Finish task." +
					" messageType:" + task.header.messageType +
					" uuid:" + task.header.uuid +
					" state:" + result.state.getValue() +
					" result:" + SystemUtil.bytesToHexString(result.data));
		} catch (NoFaceDetectException e) {
			e.printStackTrace();
			logger.info("channelRead NoFaceDetectException");
			result.state= State.NotDetectFace;
		} catch (HisignSDKException e) {
			e.printStackTrace();
			logger.info("channelRead HisignSDKException");
			result.state= State.SDKError;
		} catch (ParseParaException e) {
			e.printStackTrace();
			logger.info("channelRead ParseParaException");
			result.state= State.ParameterError;
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("channelRead otherException");
			result.state= State.OtherError;
		}finally{
			sendResult(
					task.header.messageType, 
					uuid,
					result,
					task.ctx
					);
			fetchJobFromMaster(ctx);
		}
	}

    public SDKResult doTask(HBVEMessage task) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException{
    	
    	SDKResult result = new SDKResult();
    	
    	if (HBVEMessageType.isWorkerMess(task.header.messageType)) {
    		
    		//计算相似度接口
			if (HBVEMessageType.getClientMessageType(task.header.messageType)
					.equals(HBVEMessageType.ClientMessageType.Similarity)) {
				
				ComputeSimilarityHandler computeSimilarityHandler = new ComputeSimilarityHandler();
				result = new SDKResult(State.Success, SystemUtil.int2byte(Float.floatToIntBits((float)0.98)));
				result = computeSimilarityHandler.run(task.data);
			}
			//取模板接口
			if (HBVEMessageType.getClientMessageType(task.header.messageType)
					.equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
				
				ExtractTemplateHandler extractTemplateHandler = new ExtractTemplateHandler();
				
				result = new SDKResult(State.Success, "huchengjian".getBytes());
				result = extractTemplateHandler.run(task.data);
			}
			// Todo add new task type, 可以通过反射拿到处理handler
		}
    	return result;
    }
    
    public void sendResult(byte type, byte[] uuid, SDKResult re, ChannelHandlerContext ctx){
    	
    	int messageLen = 1 + uuid.length + 1 + re.data.length; //数据包大小。 type + uuid + state + data
    	ByteBuf byteBuf = Unpooled.buffer(messageLen + 4);
    	
    	byteBuf.writeInt(messageLen);
    	
    	byteBuf.writeByte(type);
    	byteBuf.writeBytes(uuid);
    	byteBuf.writeByte(re.state.getValue());
    	byteBuf.writeBytes(re.data);

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
        sendResult((byte)currType, currUUID, new SDKResult(State.OtherError, new byte[4]), ctx);
		fetchJobFromMaster(ctx);
    }

    private byte[] getHeader() {
        return (SystemConstants.MAGIC + SystemConstants.CURRENT_VERSION).getBytes();
    }
}
