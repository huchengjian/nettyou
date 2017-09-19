package com.hisign.netty.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.hisign.THIDFaceSDK;
import com.hisign.constants.ValueConstant;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.netty.worker.SDKResult.State;
import com.hisign.netty.worker.handler.ComputeSimilarityHandler;
import com.hisign.netty.worker.handler.DetectHandler;
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
	
	private BlockingQueue<HBVEMessage> decodeQueue; //需要decode的队列
	private BlockingQueue<HBVEMessage> detectQueue; //需要detect的队列
	private BlockingQueue<DetectedBean> extractQueue; //需要extract的队列
	
	public NettyWorkerClientHandler(String name){
		tName = name;
        decodeQueue = new LinkedBlockingQueue();
        detectQueue = new LinkedBlockingQueue();
        extractQueue = new LinkedBlockingQueue();
        
        new Thread(new DetectThread()).start();
        new Thread(new ExtractThread()).start();
        for (int i = 0 ;i<SystemConstants.DECODE_THREAD_COUNT; i++) {
            new Thread(new DecodeThread()).start();
        }
	}
	
	byte[] currUUID = {};
	int currType = 0;

    static private Logger logger = LoggerFactory.getLogger(NettyWorkerClientHandler.class);
    
    private class DetectedBean{
        private THIDFaceSDK.Image images[];
        private THIDFaceSDK.Face faces[][];
        private List<HBVEMessage> taskList;
        
        public DetectedBean(THIDFaceSDK.Image images[], THIDFaceSDK.Face faces[][], List<HBVEMessage> taskList){
            this.images = images;
            this.faces = faces;
            this.taskList = taskList;
        }
    }
    
    public class DetectThread implements Runnable {
    
        public DetectThread() {
            
        }
    
        @Override
        public void run() {
            logger.info("Running DetectThread...");
    
            while (true){
                List<HBVEMessage> taskList = new ArrayList<>();
                for (int i = 0; i < SystemConstants.MAX_SDK_BATCH; i++){
                    try {
                        if (taskList.size() > 0 && detectQueue.size()==0){
                            //没有可取的task
                            break;
                        }
                        HBVEMessage task = detectQueue.take();
                        taskList.add(task);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                detectImages(taskList);
            }
        }
    }
    
    private void detectImages(List<HBVEMessage> taskList){
        
        logger.info("\ndetect Images, list size:{}\n", taskList.size());
        
        int DEFAULT_FACE_COUNT = 1;
        
        List<THIDFaceSDK.Image> imagesList = new ArrayList<>();
        List<Integer> faceCounts = new ArrayList<>();
        List<THIDFaceSDK.Rect> rects = new ArrayList<>();
        
        //轮询当前的taskList, 找出需要提取特征的image
        for (int i = 0; i < taskList.size(); i++){
            
            HBVEMessage task = taskList.get(i);
            
            
            if (HBVEMessageType.getClientMessageType(task.header.messageType)
                    .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                if (task.computeSimilarityPara.type1 == ValueConstant.IMAGE_TYPE) {
                    imagesList.add(task.computeSimilarityPara.decodeFace1);
                    faceCounts.add(DEFAULT_FACE_COUNT);
                    rects.add(null);
                }
                if (task.computeSimilarityPara.type2 ==  ValueConstant.IMAGE_TYPE) {
                    imagesList.add(task.computeSimilarityPara.decodeFace2);
                    faceCounts.add(DEFAULT_FACE_COUNT);
                    rects.add(null);
                }
            } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                    .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                imagesList.add(task.extractTemplatePara.getDecodeImg());
                faceCounts.add(DEFAULT_FACE_COUNT);
                rects.add(task.extractTemplatePara.getRect());
            } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                    .equals(HBVEMessageType.ClientMessageType.DetectFace)) {
                imagesList.add(task.extractTemplatePara.getDecodeImg());
                faceCounts.add(task.detectPara.getFaceCount());
                rects.add(null);
            }
        }
        
        THIDFaceSDK.Image images[] = castImageArray(imagesList);
        
        THIDFaceSDK.Face faces[][] = HisignFaceV9.detectBatch(faceCounts, rects, images);
    
        processDetectTask(taskList, images, faces);
    
        DetectedBean detectedBean = new DetectedBean(images, faces, taskList);
        extractQueue.offer(detectedBean);
        
//        HisignFaceV9.ImageTemplate templates[] = HisignFaceV9.getTemplates(images);
//        doTasks(taskList, templates);
    }
    
    /**
     * THIDFaceSDK.Image的list转化成array
     * @param imageBytesList
     * @return
     */
    public THIDFaceSDK.Image[] castImageArray(List<THIDFaceSDK.Image> imageBytesList){
        THIDFaceSDK.Image imagesArray[] = new THIDFaceSDK.Image[imageBytesList.size()];
        int index = 0;
        for (THIDFaceSDK.Image image : imageBytesList){
            imagesArray[index++] = image;
        }
        return imagesArray;
    }
    
    /**
     * 处理检测人脸的任务, 返回检测人脸的结果
     */
    private void processDetectTask(List<HBVEMessage> taskList, THIDFaceSDK.Image images[], THIDFaceSDK.Face faces[][]){
        
        logger.info("processDetectTask, taskCount:{}", taskList.size());
    
        List<HBVEMessage> newTaskList = new LinkedList();
        List<THIDFaceSDK.Image> newImages = new LinkedList();
        List<THIDFaceSDK.Face[]> newFaces = new LinkedList();
        
        int index = 0;
        for (int i = 0; i < taskList.size(); i++) {
            HBVEMessage task = taskList.get(i);
        
            if (!HBVEMessageType.getClientMessageType(task.header.messageType)
                    .equals(HBVEMessageType.ClientMessageType.DetectFace)) {
                newTaskList.add(task);
                if (HBVEMessageType.getClientMessageType(task.header.messageType)
                        .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                    if (task.computeSimilarityPara.type1 == 1) {
                        newImages.add(images[index]);
                        newFaces.add(faces[index]);
                    }
                    if (task.computeSimilarityPara.type2 == 1) {
                        newImages.add(images[index]);
                        newFaces.add(faces[index]);
                    }
                } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                        .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                    newImages.add(images[index]);
                    newFaces.add(faces[index]);
                }
            } else {
                SDKResult result = getDetectResult(task.header.faceCount, faces[index]);
    
                byte[] uuid = task.header.uuid.getBytes();
                sendResult(
                        task.header.messageType,
                        uuid,
                        result,
                        task.ctx
                );
            }
            index++;
        }
    
        THIDFaceSDK.Image tempImagesArray[] = new THIDFaceSDK.Image[0];
        THIDFaceSDK.Face tempFacesArray[][] = new THIDFaceSDK.Face[0][];
        
        newImages.toArray(tempImagesArray);
        newFaces.toArray(tempFacesArray);
        
        images = tempImagesArray;
        faces = tempFacesArray;
    }
    
    /**
     * SDKResult返回值格式
     *
     * state(1 byte) - count(1 byte) - face_rect(4*4*count byte:{left, top, width, height}, 每个属性占位4 byte)
     * @param faceCount
     * @param faces
     * @return
     */
    private SDKResult getDetectResult(int faceCount, THIDFaceSDK.Face faces[]){
        int count = faceCount > faces.length ? faces.length : faceCount;
        int byteCount = 1 + 4*4*count;
        
        ByteBuf byteBuf = Unpooled.buffer(byteCount);
        byteBuf.writeByte(count);
        
        for (int i = 0; i < count; i++){
            byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.left));
            byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.top));
            byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.width));
            byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.height));
        }
        
        byte data[] = new byte[byteCount];
        byteBuf.readBytes(data);
        return new SDKResult(State.Success, data);
    }
    
    
    public class ExtractThread implements Runnable{
    
        public ExtractThread(){
            
        }
    
        @Override
        public void run() {
    
            logger.info("Running ExtractThread...");
    
            while (true){
                try {
                    DetectedBean detectedBean = extractQueue.take();
                    HisignFaceV9.ImageTemplate[] templates = HisignFaceV9.extractBatch(detectedBean.images, detectedBean.faces);
                    doTasks(detectedBean.taskList, templates);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void doTasks(List<HBVEMessage> taskList, HisignFaceV9.ImageTemplate templates[]){
            
            //根据提取的特征, 将任务进行逐个的比对
            int templateIndex = 0;
            for (int i = 0; i < taskList.size(); i++){
                HBVEMessage task = taskList.get(i);
                SDKResult result = new SDKResult();
                try {
                    if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                        byte fea1[], fea2[];
                        if (task.computeSimilarityPara.type1 == 2) {
                            fea1 = task.computeSimilarityPara.getFace1();
                        }
                        else {
                            fea1 = templates[templateIndex++].template;
                        }
                        fea2 = task.computeSimilarityPara.type2 == 2 ? task.computeSimilarityPara.getFace2():templates[templateIndex++].template;
                        float score = HisignFaceV9.compareFromTwoTemplates(fea1, fea2);
    
                        result.data = SystemUtil.int2Bytes(Float.floatToIntBits(score));
                        result.state = State.Success;
                    }
                    else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
    
                        byte template[] = templates[templateIndex++].template;
                        result.data = template;
                        result.state = State.Success;
                    }else{
                        logger.error("Error Type:{}", task.header.messageType);
                    }
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
        
        byte[] sdkVersionBytes = SystemUtil.int2Bytes(Float.floatToIntBits(8.3f));
        
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
    
            decodeQueue.offer(task);
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
				result = new SDKResult(State.Success, SystemUtil.int2Bytes(Float.floatToIntBits((float)0.98)));
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
    
    private class DecodeThread implements Runnable{
    
        @Override
        public void run() {
    
            logger.info("Running DecodeThread...");
            
            while(true){
                try {
                    HBVEMessage task = decodeQueue.take();
                    if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Similarity)) {
                        
                        ComputeSimilarityHandler.ComputeSimilarityPara computeSimilarityPara =
                                ComputeSimilarityHandler.ComputeSimilarityPara.paraseData(task.data);
    
                        THIDFaceSDK.Image decodeImg1 = null, decodeImg2 = null;
                        if (computeSimilarityPara.type1 == 1){
                            decodeImg1 = HisignFaceV9.deocdeImage(computeSimilarityPara.getFace1());
                        }
                        if (computeSimilarityPara.type2 == 1){
                            decodeImg2 = HisignFaceV9.deocdeImage(computeSimilarityPara.getFace2());
                        }
                        if(decodeImg1 == null || decodeImg2 == null){
                            //TODO send
                        }
    
                        task.computeSimilarityPara = computeSimilarityPara;
                        task.computeSimilarityPara.decodeFace1 = decodeImg1;
                        task.computeSimilarityPara.decodeFace2 = decodeImg2;
                        
                        
                    }
                    else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
    
                        ExtractTemplateHandler.ExtractTemplatePara extractTemplatePara =
                                ExtractTemplateHandler.ExtractTemplatePara.paraseData(task.data);
                        THIDFaceSDK.Image decode = HisignFaceV9.deocdeImage(extractTemplatePara.getData());
    
                        if(decode == null){
                            //TODO send
                        }
                        
                        task.extractTemplatePara = extractTemplatePara;
                        task.extractTemplatePara.setDecodeImg(decode);
                    }else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.DetectFace)) {
    
                        DetectHandler.DetectPara detectPara =
                                DetectHandler.DetectPara.paraseData(task.data);
                        THIDFaceSDK.Image decodeImg = HisignFaceV9.deocdeImage(detectPara.getImgData());
    
                        if(decodeImg == null){
                            //TODO send result back
                        }
    
                        task.detectPara = detectPara;
                        task.detectPara.setDecodeImg(decodeImg);
                    }
                    detectQueue.offer(task);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ParseParaException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
