package com.hisign.netty.worker;

import com.hisign.THIDFaceSDK;
import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by hugo on 7/18/17.
 */
public class HisignFaceV9 {
    
    static private Logger log = LoggerFactory.getLogger(HisignFaceV9.class);
    
    static byte[] img1, img2;
    
    static {
        try {
            img1 = HisignBVESDK.readFile("1.jpg");
            img2 = HisignBVESDK.readFile("2.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        
    }
    
    public static void getTemplatesTest() {
        
        byte imgs[][] = new byte[2][];
        imgs[0] = img1;
        imgs[1] = img2;
        
        getTemplates(imgs);
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
    
    /**
     * @param fea1
     * @param fea2
     * @return
     * @throws HisignSDKException
     * @throws NoFaceDetectException
     * @throws ParseParaException
     * @throws IOException
     */
    public static float compareFromTwoTemplates(byte[] fea1, byte[] fea2) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException {
        float scores[] = new float[1];
        int verify = THIDFaceSDK.Verify(fea1, fea2, scores);
        log.info("verify status:{}, compare score:{}", verify, scores[0]);
        return 0;
    }
    
    /**
     * 批量获取模板数据
     * @return
     * @throws NoFaceDetectException
     * @throws ParseParaException
     * @throws IOException
     */
    public static ImageTemplate[] getTemplates(byte[][] imgBytes) {
    
        ImageTemplate templates[] = new ImageTemplate[imgBytes.length];
        
        THIDFaceSDK.Image images[] = new THIDFaceSDK.Image[2];
        for (int i = 0; i < imgBytes.length; i++) {
            THIDFaceSDK.Image tempImage = new THIDFaceSDK.Image();
            int decode = THIDFaceSDK.DecodeJPG(tempImage, imgBytes[i]);
            log.debug("decode image:{}", decode);
            images[i] = tempImage;
        }
        
        THIDFaceSDK.Face faces[][] = new THIDFaceSDK.Face[images.length][10];
        int detect = THIDFaceSDK.DetectFace(images, faces, 0, 0, null);
        log.debug("detect image:{}", detect);
        
        //获取faces每张图片的第一个人脸, 没有detect到人脸的不要获取模板
        //face_new images_new是剔除没有人脸的数组
        int successCount = getSuccessImageCount(faces);
        THIDFaceSDK.Face face_new[] = new THIDFaceSDK.Face[successCount];
        THIDFaceSDK.Image images_new[] = new THIDFaceSDK.Image[successCount];
        System.out.println("Success image count:"+successCount);
        for (int i = 0; i < faces.length; i++) {
            int faceIndex = 0;
            if (faces[i] == null || faces[i].length == 0) {
                log.debug("face i:{} is null or size 0", i);
                templates[i] = new ImageTemplate(ImageTemplate.NO_FACE);
            } else {
                log.info("face i:{} size:{}", i, faces[i].length);
                face_new[faceIndex] = faces[i][0];
                images_new[faceIndex] = images[i];
                faceIndex++;
            }
        }
        
        byte[][] features = new byte[images_new.length][];
        int extract = THIDFaceSDK.ExtractFeature(images_new, face_new, features);
        log.debug("extract image:{}", extract);
        
        for (int i = 0; i < templates.length; i++){
            int featureIndex = 0;
            if (templates[i] != null){
                templates[i] = new ImageTemplate(features[featureIndex], ImageTemplate.SUCCESSS);
                featureIndex ++;
            }
        }
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
        public int status = -1;
        
        public static final int NO_FACE = -1;
        public static final int SUCCESSS = 0;
    
        public ImageTemplate(int status){
            this.status = status;
        }
    
        public ImageTemplate(byte[] template, int status){
            this.status = status;
            this.template = template;
        }
    }
}