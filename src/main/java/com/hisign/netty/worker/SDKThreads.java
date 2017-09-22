package com.hisign.netty.worker;

import com.hisign.THIDFaceSDK;
import com.hisign.constants.SystemConstants;
import com.hisign.constants.ValueConstant;
import com.hisign.exception.ParseParaException;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.netty.worker.handler.ComputeSimilarityHandler;
import com.hisign.netty.worker.handler.DetectHandler;
import com.hisign.netty.worker.handler.ExtractTemplateHandler;
import com.hisign.util.SystemUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hugo on 9/21/17.
 */
public class SDKThreads {
    
    public static BlockingQueue<HBVEMessage> decodeQueue; //需要decode的任务队列
    private static BlockingQueue<HBVEMessage> detectQueue; //需要detect的任务队列
    private static BlockingQueue<DetectedBean> extractQueue; //需要extract的任务队列
    
    static private Logger logger = LoggerFactory.getLogger(NettyWorkerClientHandler.class);
    
    static {
        decodeQueue = new LinkedBlockingQueue();
        detectQueue = new LinkedBlockingQueue();
        extractQueue = new LinkedBlockingQueue();
    }
    
    public static void start(){
        
        if (SystemConstants.WORK_MODE == SystemConstants.GPU_MODE){
            logger.info("Working on GPU_MODE");
            for (int i = 0; i < SystemConstants.DECODE_THREAD_COUNT; i++) {
                new Thread(new DecodeThread()).start();
            }
    
            for (int i = 0; i < SystemConstants.DETECT_THREAD_COUNT; i++) {
                new Thread(new DetectThread()).start();
            }
    
            for (int i = 0; i < SystemConstants.EXTRACT_THREAD_COUNT; i++) {
                new Thread(new ExtractThread()).start();
            }
        }else if (SystemConstants.WORK_MODE == SystemConstants.CPU_MODE){
            logger.info("Working on CPU_MODE");
            for (int i = 0; i < SystemConstants.DECODE_THREAD_COUNT; i++) {
                new Thread(new DecodeThread()).start();
            }
        }
    }
    
    private static class DecodeThread implements Runnable{
        
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
                            if(decodeImg1 == null){
                                logger.error("Similarity task, Decode image1 error, img length:{}", computeSimilarityPara.getFace1().length);
                                SDKResult result = new SDKResult(SDKResult.State.Image1Error, new byte[0]);
                                sendResult(task, result);
                                continue;
                            }
                        }
                        if (computeSimilarityPara.type2 == 1){
                            decodeImg2 = HisignFaceV9.deocdeImage(computeSimilarityPara.getFace2());
                            if(decodeImg2 == null){
                                logger.error("Similarity task, Decode image2 error, img length:{}", computeSimilarityPara.getFace2().length);
                                SDKResult result = new SDKResult(SDKResult.State.Image2Error, new byte[0]);
                                sendResult(task, result);
                                continue;
                            }
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
                            logger.error("Extract_Template Task, Decode image error, img length:{}", extractTemplatePara.getData().length);
                            SDKResult result = new SDKResult(SDKResult.State.ImageError, new byte[0]);
                            sendResult(task, result);
                            continue;
                        }
                        
                        task.extractTemplatePara = extractTemplatePara;
                        task.extractTemplatePara.setDecodeImg(decode);
                    }else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.DetectFace)) {
                        
                        DetectHandler.DetectPara detectPara =
                                DetectHandler.DetectPara.paraseData(task.data);
                        THIDFaceSDK.Image decodeImg = HisignFaceV9.deocdeImage(detectPara.getImgData());
                        
                        if(decodeImg == null){
                            logger.error("DetectFace Task, Decode image error, img length:{}", detectPara.getImgData().length);
                            SDKResult result = new SDKResult(SDKResult.State.ImageError, new byte[0]);
                            sendResult(task, result);
                            continue;
                        }
                        task.detectPara = detectPara;
                        task.detectPara.setDecodeImg(decodeImg);
                    }
                    
                    if (SystemConstants.WORK_MODE == SystemConstants.CPU_MODE){
                        List<HBVEMessage> list = new ArrayList<HBVEMessage>();
                        list.add(task);
                        DetectThread.detectImages(list);
                    }else if (SystemConstants.WORK_MODE == SystemConstants.GPU_MODE){
                        detectQueue.offer(task);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ParseParaException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    private static class DetectedBean{
        private THIDFaceSDK.Image images[];
        private THIDFaceSDK.Face faces[][];
        private List<HBVEMessage> taskList;
        
        public DetectedBean(THIDFaceSDK.Image images[], THIDFaceSDK.Face faces[][], List<HBVEMessage> taskList){
            this.images = images;
            this.faces = faces;
            this.taskList = taskList;
        }
    }
    
    public static class DetectThread implements Runnable {
        
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
    
        public static void detectImages(List<HBVEMessage> taskList){
        
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
                        faceCounts.add(HisignFaceV9.DefaultCount);
                        rects.add(null);
                    }
                    if (task.computeSimilarityPara.type2 ==  ValueConstant.IMAGE_TYPE) {
                        imagesList.add(task.computeSimilarityPara.decodeFace2);
                        faceCounts.add(HisignFaceV9.DefaultCount);
                        rects.add(null);
                    }
                } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                        .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                    imagesList.add(task.extractTemplatePara.getDecodeImg());
                    faceCounts.add(HisignFaceV9.DefaultCount);
                    rects.add(task.extractTemplatePara.getRect());
                } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                        .equals(HBVEMessageType.ClientMessageType.DetectFace)) {
                    imagesList.add(task.detectPara.getDecodeImg());
                    faceCounts.add(task.detectPara.getFaceCount());
                    rects.add(null);
                }
            }
        
            THIDFaceSDK.Image images[] = castImageArray(imagesList);
        
            THIDFaceSDK.Face faces[][] = HisignFaceV9.detectBatch(faceCounts, rects, images);
        
            DetectedBean detectedBean = processDetectTask(taskList, images, faces);
        
            if (detectedBean.taskList.size()!=0){
                if (SystemConstants.WORK_MODE == SystemConstants.CPU_MODE){
                    ExtractThread.doExtract(detectedBean);
                }else if (SystemConstants.WORK_MODE == SystemConstants.GPU_MODE){
                    extractQueue.offer(detectedBean);
                }
            }
        }
    
        /**
         * THIDFaceSDK.Image的list转化成array
         * @param imageBytesList
         * @return
         */
        private static THIDFaceSDK.Image[] castImageArray(List<THIDFaceSDK.Image> imageBytesList){
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
        private static DetectedBean processDetectTask(List<HBVEMessage> taskList, THIDFaceSDK.Image images[], THIDFaceSDK.Face faces[][]){
        
            logger.info("processDetectTask, taskCount:{}", images.length);
        
            List<HBVEMessage> newTaskList = new ArrayList();
            List<THIDFaceSDK.Image> newImages = new ArrayList();
            List<THIDFaceSDK.Face[]> newFaces = new ArrayList<>();
        
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
                            index++;
                        }
                        if (task.computeSimilarityPara.type2 == 1) {
                            newImages.add(images[index]);
                            newFaces.add(faces[index]);
                            index++;
                        }
                    } else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                        newImages.add(images[index]);
                        newFaces.add(faces[index]);
                        index++;
                    }
                } else {
                    SDKResult result = getDetectResult(task.detectPara.getFaceCount(), faces[index]);
                    index++;
                
                    sendResult(task, result);
                }
            
            }
        
            THIDFaceSDK.Image tempImagesArray[] = new THIDFaceSDK.Image[newImages.size()];
            THIDFaceSDK.Face tempFacesArray[][] = new THIDFaceSDK.Face[newFaces.size()][];
        
            newImages.toArray(tempImagesArray);
            newFaces.toArray(tempFacesArray);
        
            return new DetectedBean(tempImagesArray, tempFacesArray, newTaskList);
        }
    
        /**
         * SDKResult返回值格式
         *
         * state(1 byte) - count(1 byte) - face_rect(4*4*count byte:{left, top, width, height}, 每个属性占位4 byte)
         * @param faceCount
         * @param faces
         * @return
         */
        private static SDKResult getDetectResult(int faceCount, THIDFaceSDK.Face faces[]){
        
            if (faces.length == 0){
                return new SDKResult(SDKResult.State.NotDetectFace, new byte[0]);
            }
        
            int count = faceCount > faces.length ? faces.length : faceCount;
            int byteCount = 1 + 4*5*count;
        
            ByteBuf byteBuf = Unpooled.buffer(byteCount);
            byteBuf.writeByte(count);
        
            for (int i = 0; i < count; i++){
                byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.left));
                byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.top));
                byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.width));
                byteBuf.writeBytes(SystemUtil.int2Bytes(faces[i].rect.height));
                byteBuf.writeBytes(SystemUtil.float2Bytes(faces[i].confidence));
            }
        
            byte data[] = new byte[byteCount];
            byteBuf.readBytes(data);
            return new SDKResult(SDKResult.State.Success, data);
        }
        
    }
    
    public static class ExtractThread implements Runnable{
        
        public ExtractThread(){
            
        }
        
        @Override
        public void run() {
            
            logger.info("Running ExtractThread...");
            
            while (true){
                try {
                    DetectedBean detectedBean = extractQueue.take();
                    doExtract(detectedBean);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        public static void doExtract(DetectedBean detectedBean ){
            HisignFaceV9.ImageTemplate[] templates = HisignFaceV9.extractBatch(detectedBean.images, detectedBean.faces);
            doExtract1(detectedBean.taskList, templates);
        }
        
        private static void doExtract1(List<HBVEMessage> taskList, HisignFaceV9.ImageTemplate templates[]){
            
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
                        result.state = SDKResult.State.Success;
                    }
                    else if (HBVEMessageType.getClientMessageType(task.header.messageType)
                            .equals(HBVEMessageType.ClientMessageType.Extract_Template)) {
                        
                        HisignFaceV9.ImageTemplate imageTemplate = templates[templateIndex++];
                        result.data = imageTemplate.template;
                        result.state = imageTemplate.state;
                    }else{
                        logger.error("Error Type:{}", task.header.messageType);
                    }
                }finally {
                    sendResult(task, result);
                }
            }
        }
    }
    
    public static void sendResult(HBVEMessage task, SDKResult result){
        byte[] uuid = task.header.uuid.getBytes();
        sendResult(task.header.messageType, uuid, result, task.ctx);
    }
    
    public static void sendResult(byte type, byte[] uuid, SDKResult re, ChannelHandlerContext ctx){
        
        logger.info("Send Result to master, type:{}, re.data len:{}", type, re.data.length);
        
        int messageLen = 1 + uuid.length + 1 + re.data.length; //数据包大小。 type + uuid + state + data
        ByteBuf byteBuf = Unpooled.buffer(messageLen + 4);
        
        byteBuf.writeInt(messageLen);
        
        byteBuf.writeByte(type);
        byteBuf.writeBytes(uuid);
        byteBuf.writeByte(re.state.getValue());
        byteBuf.writeBytes(re.data);
        
        ctx.writeAndFlush(byteBuf);
    }
}
