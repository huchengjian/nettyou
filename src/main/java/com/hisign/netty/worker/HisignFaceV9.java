package com.hisign.netty.worker;

import com.hisign.THIDFaceSDK;
import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by hugo on 7/18/17.
 */
public class HisignFaceV9 {
    
    public final static int DefaultCount = 1;
    public final static int MaximumCount = 20;
    
    static private Logger log = LoggerFactory.getLogger(HisignFaceV9.class);
    
    static byte[] img1, img2;
    
    public static void main(String[] args) {
        
    }
    
    /**
     * @param fea1
     * @param fea2
     * @return
     * @throws HisignSDKException
     * @throws NoFaceDetectException
     * @throws ParseParaException
     * @throws IOException
     */
    public static float compareFromTwoTemplates(byte[] fea1, byte[] fea2) {
        float scores[] = new float[1];
        try{
            int verify = THIDFaceSDK.Verify(fea1, fea2, scores);
            log.info("verify status:{}, compare score:{}", verify, scores[0]);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return scores[0];
        }
    }
    
    /**
     *
     * @param img
     * @return
     */
    public static THIDFaceSDK.Image deocdeImage(byte[] img){
        THIDFaceSDK.Image tempImage = new THIDFaceSDK.Image();
        int decode = THIDFaceSDK.DecodeJPG(tempImage, img);
        log.debug("Decode image status:{}", decode);
        if (decode != 0){
            tempImage = null;
        }
        return tempImage;
    }
    
    /**
     *
     * @param img
     * @return
     */
    public static THIDFaceSDK.Image deocdeImageTest(byte[] img){
        THIDFaceSDK.Image tempImage = new THIDFaceSDK.Image();
        return tempImage;
    }
    
    public static THIDFaceSDK.Face[][] detectBatch(List<Integer> faceCountList, List<THIDFaceSDK.Rect> rects, THIDFaceSDK.Image images[]){
    
        THIDFaceSDK.Face faces[][] = new THIDFaceSDK.Face[images.length][];
    
        THIDFaceSDK.Rect rectArray[] = new THIDFaceSDK.Rect[rects.size()];
        rects.toArray(rectArray);
        
        for (int i = 0;i<faceCountList.size();i++){
            
            faces[i] = new THIDFaceSDK.Face[faceCountList.get(i)];
            
            //够照默认的react, 若rect为空会导致DetectFace出错
            if (rectArray[i] == null){
                rectArray[i] = new THIDFaceSDK.Rect();
                rectArray[i].left = 0;
                rectArray[i].top = 0;
                rectArray[i].width = images[i] == null ? 0 : images[i].width;
                rectArray[i].height = images[i] == null ? 0 : images[i].height;
            }
        }
    
        int detect = THIDFaceSDK.DetectFace(images, faces, 0, 0, rectArray);
        log.debug("Detect image status:{}", detect);
    
//        for (int i = 0; i< images.length; i++){
//            System.out.printf("%s---image raw len:%d, faces count:%d, rect:%s\n", i, images[i].raw.length, faces[i].length, rectArray[i].toString());
//        }
        
        return faces;
    }
    
    public static ImageTemplate[] extractBatch(THIDFaceSDK.Image images[], THIDFaceSDK.Face faces[][]){
        
        log.info("ExtarctBatch, list size:{}\n", images.length);
    
        ImageTemplate templates[] = new ImageTemplate[images.length];
    
        //获取faces每张图片的第一个人脸, 没有detect到人脸的不要获取模板
        //face_new images_new是剔除没有人脸的数组
        int successCount = getSuccessImageCount(faces);
        THIDFaceSDK.Face face_new[] = new THIDFaceSDK.Face[successCount];
        THIDFaceSDK.Image images_new[] = new THIDFaceSDK.Image[successCount];
        log.info("Success detect image count:" + successCount);
        
        int faceIndex = 0;
        for (int i = 0; i < faces.length; i++) {
            if (faces[i] == null || faces[i].length == 0) {
                log.debug("face i:{} is null or size 0", i);
                templates[i] = new ImageTemplate(SDKResult.State.NotDetectFace);
            } else {
//                log.info("face index:{} size:{}", i, faces[i].length);
                face_new[faceIndex] = faces[i][0];
                images_new[faceIndex] = images[i];
                faceIndex++;
            }
        }
    
        log.debug("images_new size:{}, face_new size:{}", images_new.length, face_new.length);
    
        byte[][] features = new byte[images_new.length][];
        int extract = THIDFaceSDK.ExtractFeature(images_new, face_new, features);
        log.debug("extract image status:{}", extract);
    
        int featureIndex = 0;
        for (int i = 0; i < templates.length; i++){
            if (templates[i] == null){
                templates[i] = new ImageTemplate(features[featureIndex++], SDKResult.State.Success);
            }
        }
        log.debug("template size:{}", templates.length);
        return templates;
    }
    
    
    /**
     * 批量获取模板数据
     * @return
     * @throws NoFaceDetectException
     * @throws ParseParaException
     * @throws IOException
     */
    public static ImageTemplate[] getTemplates(THIDFaceSDK.Image images[]) {
        
        log.info("Process get template list, size:{}", images.length);
    
//        THIDFaceSDK.Image images[] = new THIDFaceSDK.Image[imgBytes.length];
//        for (int i = 0; i < imgBytes.length; i++) {
//            THIDFaceSDK.Image tempImage = new THIDFaceSDK.Image();
//            int decode = THIDFaceSDK.DecodeJPG(tempImage, imgBytes[i]);
//            log.debug("decode image:{}", decode);
//            images[i] = tempImage;
//        }
        
        ImageTemplate templates[] = new ImageTemplate[images.length];
        
        THIDFaceSDK.Face faces[][] = new THIDFaceSDK.Face[images.length][1];
        int detect = THIDFaceSDK.DetectFace(images, faces, 0, 0, null);
        log.debug("detect image:{}", detect);


//        for (int i = 0; i< images.length; i++){
//            THIDFaceSDK.Image tempImage[] = new THIDFaceSDK.Image[1];
//            tempImage[0] = images[i];
//            THIDFaceSDK.Face tempFace[][] = new THIDFaceSDK.Face[1][1];
//
//            int min_face = tempImage[0].height < tempImage[0].width ? tempImage[0].height/4 : tempImage[0].width/4;
//            log.debug("Min face:{}", min_face);
//            int detect = THIDFaceSDK.DetectFace(tempImage, tempFace, min_face, 0, null);
//            log.info("Detect image status:{}, batchSize:{}", detect, tempImage.length);
//
//            if (tempFace[0] == null || tempFace[0].length == 0){
//                detect = THIDFaceSDK.DetectFace(tempImage, tempFace, 0, 0, null);
//                log.debug("Re-Detect image status:{}, batchSize:{}, min_face:0", detect, tempImage.length);
//            }
//
//            images[i] = tempImage[0];
//            faces[i] = tempFace[0];
//        }
        
        
        //获取faces每张图片的第一个人脸, 没有detect到人脸的不要获取模板
        //face_new images_new是剔除没有人脸的数组
        int successCount = getSuccessImageCount(faces);
        THIDFaceSDK.Face face_new[] = new THIDFaceSDK.Face[successCount];
        THIDFaceSDK.Image images_new[] = new THIDFaceSDK.Image[successCount];
        log.info("Success image count:" + successCount);
        int faceIndex = 0;
        for (int i = 0; i < faces.length; i++) {
            if (faces[i] == null || faces[i].length == 0) {
                log.debug("face i:{} is null or size 0", i);
                templates[i] = new ImageTemplate(SDKResult.State.NotDetectFace);
            } else {
                log.info("face index:{} size:{}", i, faces[i].length);
                face_new[faceIndex] = faces[i][0];
                images_new[faceIndex] = images[i];
                faceIndex++;
            }
        }
        
        log.debug("images_new size:{}, face_new size:{}", images_new.length, face_new.length);
        
        byte[][] features = new byte[images_new.length][];
        int extract = THIDFaceSDK.ExtractFeature(images_new, face_new, features);
        log.debug("extract image status:{}", extract);
        
        int featureIndex = 0;
        for (int i = 0; i < templates.length; i++){
            if (templates[i] == null){
                templates[i] = new ImageTemplate(features[featureIndex++], SDKResult.State.Success);
            }
            log.info("Image:{} template is null", i);
        }
        log.debug("template size:{}", templates.length);
        return templates;
    }
    
    public static int getSuccessImageCount(THIDFaceSDK.Face faces[][]){
        int count = 0;
        for (int i = 0; i < faces.length; i++) {
            if (faces[i] == null || faces[i].length == 0) {
                count++;
            }
        }
        return faces.length-count;
    }
    
    public static class ImageTemplate{
        public byte[] template;
        public SDKResult.State state;
        
        public ImageTemplate(SDKResult.State state){
            this.state = state;
            this.template = new byte[0];
        }
    
        public ImageTemplate(byte[] template, SDKResult.State state){
            this.state = state;
            this.template = template;
        }
    }
    
    public static ImageTemplate[]  getTemplatesTest(THIDFaceSDK.Image images[]){
        ImageTemplate result[] = new ImageTemplate[images.length];
        int index = 0;
        for (ImageTemplate image : result){
            image = new ImageTemplate(SDKResult.State.NotDetectFace);
            result[index++] = image;
        }
        return result;
    }
    
    public static float compareFromTwoTemplatesTest(byte[] fea1, byte[] fea2) {
        return 0.99f;
    }
    
    public static void test() throws IOException {
        
        System.out.println("test method!");
        
        THIDFaceSDK.Image image1 = new THIDFaceSDK.Image();
        int a1 = THIDFaceSDK.DecodeJPG(image1, img1);
        
        THIDFaceSDK.Image image2 = new THIDFaceSDK.Image();
        int a2 = THIDFaceSDK.DecodeJPG(image2, img2);
        
        System.out.println(a1 + ":" + a2);
        
        THIDFaceSDK.Image imgs[] = new THIDFaceSDK.Image[2];
        imgs[0] = image1;
        imgs[1] = image2;
        
        THIDFaceSDK.Face faces[][] = new THIDFaceSDK.Face[2][10];
        int detect = THIDFaceSDK.DetectFace(imgs, faces, 0, 0, null);
        
        System.out.println("detect:" + detect);
        
        THIDFaceSDK.Face face_new[] = new THIDFaceSDK.Face[2];
        face_new[0] = faces[0][0];
        face_new[1] = faces[1][0];
        
        byte[][] features = new byte[2][];
        int ex = THIDFaceSDK.ExtractFeature(imgs, face_new, features);
        System.out.println("extract:" + ex);
        
        float scores[] = new float[10];
        int verify = THIDFaceSDK.Verify(features[0], features[1], scores);
        System.out.println("verify:" + verify);
        
        for (float sc : scores) {
            System.out.println("print score:" + sc);
        }
    }
}
