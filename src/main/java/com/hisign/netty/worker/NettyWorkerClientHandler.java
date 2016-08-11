package com.hisign.netty.worker;

import com.alibaba.fastjson.JSON;
import com.hisign.netty.service.RequestService;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.ClientResult;
import com.hisign.bean.Message;
import com.hisign.bean.WorkerRequest;
import com.hisign.bean.WorkerResultRequest;
import com.hisign.constants.Status;
import com.hisign.constants.SystemConstants;
import com.hisign.exception.HisignSDKException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

//        RequestService.addValidateFields(jo);

        if (isFirstReq) {
            request.writeBytes(getHeader());
        }
        
        request.writeInt(1);
        request.writeByte(2);
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
			WorkerRequest workerRequest = parsePara(req);

			connId = workerRequest.uuid_connId;

//			int status = para.getInteger(Message.Status);
//			if (status < 0) {
//				logger.info("Worker 收到消息错误:" + body);
//				ctx.close();
//				return;
//			}

			float score = (float) 0.98;
//			score = compute(
//					workerRequest.clientRequest.getType1(),
//					workerRequest.clientRequest.getType2(),
//					workerRequest.clientRequest.getFace1(),
//					workerRequest.clientRequest.getFace2()
//					);
			WorkerResultRequest workerResultRequest = new WorkerResultRequest();
			workerResultRequest.setChannelHandlerContext(ctx);
			workerResultRequest.setScore(score);
			workerResultRequest.setStatus(0);
			workerResultRequest.setUuid_connId(connId);
			workerResultRequest.setStatusMessage("");
			sendResult(workerResultRequest);
			
//			JSONObject result = composeResultToMaster(connId, score);
//			logger.info("Worker finish task, result:" + result.toJSONString());
//			sendMessage(result.toJSONString(), ctx);

		} catch (IOException e) {
			sendMessage(getErrorMessage(Status.ParaError, Status.ParaErrorMessg, connId), ctx);
		}
//		catch (HisignSDKException e) {
//			logger.error("Hisign sdk compute error!");
//			sendMessage(getErrorMessage(Status.ComputeError, Status.ComputeErrorMessg, connId), ctx);
//		}
		catch (Exception e) {
			logger.error("Hisign sdk compute error!");
//			sendMessage(getErrorMessage(Status.ComputeError, Status.ComputeErrorMessg, connId), ctx);
		}
		finally{
			fetchJobFromMaster(ctx);
		}
	}
    
    private void sendResult(WorkerResultRequest workerResultRequest){
    	
    	ByteBuf byteBuf = Unpooled.buffer(1024);
    	byteBuf.writeInt(workerResultRequest.getSize());
    	System.out.println("result length" + workerResultRequest.getSize());
    	
    	byteBuf.writeByte(workerResultRequest.messageType);
    	byteBuf.writeByte(0);
    	byteBuf.writeFloat(workerResultRequest.getScore());
    	
    	byteBuf.writeInt(workerResultRequest.getUuid_connId().length());
    	byteBuf.writeBytes(workerResultRequest.getUuid_connId().getBytes());
    	
    	byteBuf.writeInt(workerResultRequest.getStatusMessage().getBytes().length);
    	byteBuf.writeBytes(workerResultRequest.getStatusMessage().getBytes());
    	workerResultRequest.getChannelHandlerContext().writeAndFlush(byteBuf);
    }
    
    private WorkerRequest parsePara(byte[] para) throws UnsupportedEncodingException {
    	
    	int point = 0;
    	
    	WorkerRequest workerRequest = new WorkerRequest();
    	workerRequest.message_type = SystemUtil.singleByteToInt(para[point]);
    	point ++;

    	int connLength = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, point, point+4));
    	point += 4;
    	workerRequest.uuid_connId = new String(Arrays.copyOfRange(para, point, point+connLength));
    	point+=connLength;

    	workerRequest.clientRequest.setType1(para[point]);
    	point++;
    	workerRequest.clientRequest.setType2(para[point]);
    	point++;
    	
    	int face1Length = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, point, point+4));
    	point += 4;
    	workerRequest.clientRequest.setFace1(Arrays.copyOfRange(para, point, point+face1Length));
    	point+=face1Length;
    	
    	int face2Length = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, point, point+4));
    	point += 4;
    	workerRequest.clientRequest.setFace2(Arrays.copyOfRange(para, point, point+face2Length));

    	return workerRequest;
	}
    
    private void printMess(String body) {
    	String mess = body.length() > 200 ?
    			body.substring(0, 200) : body;
    	
    	logger.info("Worker Server Receive Message." + "fulllength:"+ body.length() + "\n" + mess);
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
    
    private float compute(int type1, int type2, byte[] face1, byte[] face2) throws HisignSDKException, IOException{

    	logger.info("compute similarity!");

		byte[] temp1 = face1, temp2 = face2;
    	if (type1 == 1) {
    		//图片數據
			temp1 = HisignBVESDK.getTemplateByImageByteArray(face1);
        }
    	if (type2 == 1) {
    		//图片數據
			temp2 = HisignBVESDK.getTemplateByImageByteArray(face2);
        }
    	
    	logger.info("size of template:"+temp1.length + " "+temp2.length);
    	return HisignBVESDK.compareFromTwoTemplate(temp1, temp2);
    }
    
    private void sendMessage(String resultMessage, ChannelHandlerContext chx) {
//		resultMessage = SystemUtil.addNewLine(resultMessage);
		ByteBuf byteBuf = Unpooled.buffer(1024);
		byteBuf.writeInt(resultMessage.length());
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
