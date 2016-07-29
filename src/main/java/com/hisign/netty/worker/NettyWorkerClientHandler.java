package com.hisign.netty.worker;

import com.alibaba.fastjson.JSON;
import com.hisign.netty.service.RequestService;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;
import com.hisign.constants.Status;
import com.hisign.constants.SystemConstants;
import com.hisign.exception.HisignSDKException;

import java.io.IOException;
import java.util.Arrays;

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
        request = Unpooled.buffer(1024);

        JSONObject jo = new JSONObject();
        jo.put(Message.MessageType, 2);
        RequestService.addValidateFields(jo);

        if (isFirstReq) {
            request.writeBytes(getHeader());
        }
        request.writeBytes(SystemUtil.addNewLine(jo.toJSONString()).getBytes());
        ctx.writeAndFlush(request);
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
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		logger.info(ctx.channel().remoteAddress() + "：" + msg);
		logger.info("worker channelRead..");
		String connId = "";
		
		try {

			ByteBuf buf = (ByteBuf) msg;
			byte[] req = new byte[buf.readableBytes()];
			buf.readBytes(req);

			String body = new String(req, "UTF-8");
			logger.info("worker receive message:"+body);

			JSONObject para = JSON.parseObject(body);
			connId = para.getString(Message.ConnId);

			int status = para.getInteger(Message.Status);
			if (status < 0) {
				logger.info("Worker 收到消息错误:" + body);
				ctx.close();
				return;
			}

			JSONObject connData = JSONObject.parseObject(para
					.getString(Message.DATA));
			float score = (float) 0.98;
			score = compute(connData);

			JSONObject result = composeResultToMaster(connId, score);
			logger.info("Worker finish task, result:" + result.toJSONString());
			sendMessage(result.toJSONString(), ctx);

		} catch (IOException e) {
			sendMessage(getErrorMessage(Status.ParaError, Status.ParaErrorMessg, connId), ctx);
		}
		catch (HisignSDKException e) {
			logger.error("Hisign sdk compute error!");
			sendMessage(getErrorMessage(Status.ComputeError, Status.ComputeErrorMessg, connId), ctx);
		}
		catch (Exception e) {
			logger.error("Hisign sdk compute error!");
			sendMessage(getErrorMessage(Status.ComputeError, Status.ComputeErrorMessg, connId), ctx);
		}
		finally{
			fetchJobFromMaster(ctx);
		}
	}
    
    private JSONObject composeResultToMaster(String connId, double score) {
        JSONObject result = new JSONObject();
        result.put(Message.MessageType, 3);
        result.put(Message.ConnId, connId);
        result.put(Message.Score, score);
        result.put(Message.Status, 0);
        RequestService.addValidateFields(result);
        return result;
	}
    
    private float compute(JSONObject data) throws HisignSDKException, IOException{

    	logger.info("compute similarity!");

    	String  verify1 = data.getString(Message.Verify1);
    	String  verify2 = data.getString(Message.Verify2);
    	int  type1 = data.getIntValue(Message.Type1);
    	int  type2 = data.getIntValue(Message.Type2);
    	
    	sun.misc.BASE64Decoder decoder = new  sun.misc.BASE64Decoder();
    	
		byte[] byte1 = null;
		byte[] byte2 = null;

		byte1 = decoder.decodeBuffer(verify1);
		byte2 = decoder.decodeBuffer(verify2);

		byte[] temp1 = byte1, temp2 = byte2;
    	if (type1 == 1) {
    		//图片數據
			temp1 = HisignBVESDK.getTemplateByImageByteArray(byte1);
        }
    	if (type2 == 1) {
    		//图片數據
			temp2 = HisignBVESDK.getTemplateByImageByteArray(byte2);
        }
    	
    	logger.info("size of template:"+temp1.length + " "+temp2.length);
    	return HisignBVESDK.compareFromTwoTemplate(temp1, temp2);
    }
    
    private void sendMessage(String resultMessage, ChannelHandlerContext chx) {
		resultMessage = SystemUtil.addNewLine(resultMessage);
		ByteBuf byteBuf = Unpooled.buffer(1024);
		byteBuf.writeBytes(resultMessage.getBytes());
		chx.writeAndFlush(byteBuf);
	}

	private String getErrorMessage(int status, String messg, String connId){
    	
    	JSONObject jo = new JSONObject();
    	jo.put(Message.Status, status);
    	jo.put(Message.StatusMessage, messg);
		jo.put(Message.ConnId, connId);

		jo.put(Message.MessageType, 3);

		return jo.toJSONString();
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
//        System.out.println("ReadComplete");
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

    private byte[] getHeader() {
        return (SystemConstants.MAGIC + SystemConstants.CURRENT_VERSION).getBytes();
    }
}
