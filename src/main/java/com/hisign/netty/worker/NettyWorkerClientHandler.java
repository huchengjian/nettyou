package com.hisign.netty.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
	
	public static int maxTaskCount = 10;
	
	private BlockingQueue<HBVEMessage> taskQueue;
	
	public NettyWorkerClientHandler(String name){
		tName = name;
        taskQueue = new LinkedBlockingQueue();
        
        new Thread(new ProcessThread()).start();
	}
	
	byte[] currUUID = {};
	int currType = 0;

    static private Logger logger = LoggerFactory.getLogger(NettyWorkerClientHandler.class);
    
    private class ProcessThread implements Runnable{
    
        public ProcessThread(){
            
        }
    
        @Override
        public void run() {
            while (true){
                List<HBVEMessage> taskList = new ArrayList<HBVEMessage>();
                for (int i = 0; i < maxTaskCount; i++){
                    try {
                        if (taskList.size() > 0 && taskQueue.size()==0){
                            //没有可取的task
                            break;
                        }
                        HBVEMessage task = taskQueue.take();
                        taskList.add(task);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                processTaskList(taskList);
            }
        }
        
        private void processTaskList(List<HBVEMessage> taskList){
    
            logger.info("Process task list, list size:{}", taskList.size());
            
            List<byte[]> imageBytesList = new ArrayList<>();
            
            //轮询当前的taskList, 找出需要提取特征的
            for (int i = 0; i < taskList.size(); i++){
                
                HBVEMessage task = taskList.get(i);
                try {
                    if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                        ComputeSimilarityHandler.ComputeSimilarityPara computeSimilarityPara =
                                ComputeSimilarityHandler.ComputeSimilarityPara.paraseData(task.data);
                        if (computeSimilarityPara.type1 == 1) {
                            imageBytesList.add(computeSimilarityPara.face1);
                        }
                        if (computeSimilarityPara.type1 == 1) {
                            imageBytesList.add(computeSimilarityPara.face2);
                        }
                    }
                    else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                        ExtractTemplateHandler.ExtractTemplatePara extractTemplatePara =
                                ExtractTemplateHandler.ExtractTemplatePara.paraseData(task.data);
                        imageBytesList.add(extractTemplatePara.getData());
                    }
                }catch (ParseParaException e) {
                    e.printStackTrace();
                }
            }
            
            byte imageBytesArray[][] = castImageArray(imageBytesList);
            HisignFaceV9.ImageTemplate templates[] = HisignFaceV9.getTemplates(imageBytesArray);
    
            doTasks(taskList, templates);
        }
        
        public byte[][] castImageArray(List<byte[]> imageBytesList){
            byte[][] templates = new byte[imageBytesList.size()][];
            int index = 0;
            for (byte[] image : imageBytesList){
                templates[index++] = image;
            }
            return templates;
        }
        
        private void doTasks(List<HBVEMessage> taskList, HisignFaceV9.ImageTemplate templates[]){
            int templateIndex = 0;
            for (int i = 0; i < taskList.size(); i++){
                HBVEMessage task = taskList.get(i);
                SDKResult result = new SDKResult();
                try {
                    if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                        ComputeSimilarityHandler.ComputeSimilarityPara computeSimilarityPara =
                                ComputeSimilarityHandler.ComputeSimilarityPara.paraseData(task.data);
                        byte fea1[], fea2[] = null;
                        if (computeSimilarityPara.type1 == 2) {
                            fea1 = computeSimilarityPara.getFace1();
                        }
                        else {
                            fea1 = templates[templateIndex++].template;
                        }
                        fea2 = computeSimilarityPara.type2 == 2 ? computeSimilarityPara.getFace2():templates[templateIndex++].template;
                        float score = HisignFaceV9.compareFromTwoTemplates(fea1, fea2);
    
                        result.data = SystemUtil.int2byte(Float.floatToIntBits(score));
                        result.state = State.Success;
                    }
                    else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
    
                        byte template[] = templates[templateIndex++].template;
                        result.data = template;
                        result.state = State.Success;
                    }
                }catch (ParseParaException e) {
                    e.printStackTrace();
                    result.state = State.ParameterError;
                }finally {
                    byte[] uuid = task.header.uuid.getBytes();
                    sendResult(
                            task.header.messageType,
                            uuid,
                            result,
                            task.ctx
                    );
                }
            }
        }
    }
    
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
        
        try{
            HBVEMessage task = (HBVEMessage) msg;
            byte[] uuid = task.header.uuid.getBytes();
            currUUID = uuid;
            currType = task.header.messageType;
            task.ctx = ctx;
    
            logger.info(ctx.channel().remoteAddress() + "：" + msg);
            logger.info(tName + " thread. worker channelRead.." + " messageType:" +
                    task.header.messageType + " uuid:" + task.header.uuid + " para_len:" + task.data.length);
    
            taskQueue.offer(task);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
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
    
        logger.info("Send Result to master, type:{}, re.data:{}", type, SystemUtil.bytesToHexString(re.data));
    
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
