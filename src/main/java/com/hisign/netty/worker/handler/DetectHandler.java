package com.hisign.netty.worker.handler;

import com.hisign.THIDFaceSDK;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignFaceV9;

/**
 * Created by hugo on 9/18/17.
 */
public class DetectHandler extends WorkerHandler{
    
    public static class DetectPara {
        private byte[] imgData;
        
        private THIDFaceSDK.Image decodeImg;
    
        private int faceCount;
    
        public int getFaceCount() {
            return faceCount;
        }
    
        public void setFaceCount(int faceCount) {
            this.faceCount = faceCount;
        }
        
        public void setDecodeImg(THIDFaceSDK.Image decodeImg) {
            this.decodeImg = decodeImg;
        }
        public THIDFaceSDK.Image getDecodeImg() {
            return decodeImg;
        }
        
        public void setImgData(byte[] data) {
            this.imgData = data;
        }
        public byte[] getImgData() {
            return imgData;
        }
    
        /**
         * 参数格式faceCount + imgData
         * @param para
         * @return
         * @throws ParseParaException
         */
        public static DetectHandler.DetectPara paraseData(byte[] para)
                throws ParseParaException {
            DetectHandler.DetectPara detectPar = new DetectHandler.DetectPara();
    
            //默认的人脸数量判断, 超出MaximumCount判断
            int faceCount = getByte(para, 0);
            if (faceCount <= 0){
                faceCount = HisignFaceV9.DefaultCount;
            }else if (faceCount >HisignFaceV9.MaximumCount){
                faceCount = HisignFaceV9.MaximumCount;
            }
            
            detectPar.setFaceCount(faceCount);
            detectPar.setImgData(getBytes(para, 1, para.length));
            return detectPar;
        }
    }
}
