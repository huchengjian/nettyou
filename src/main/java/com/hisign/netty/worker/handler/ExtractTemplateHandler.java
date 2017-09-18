package com.hisign.netty.worker.handler;

import java.io.IOException;

import com.hisign.THIDFaceSDK;
import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignBVESDK;
import com.hisign.netty.worker.SDKResult;
import com.hisign.netty.worker.SDKResult.State;
import com.hisign.util.SystemUtil;

public class ExtractTemplateHandler extends WorkerHandler {
	
	public SDKResult run(byte[] data) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException{
		SDKResult result = new SDKResult();
		byte[] re = null;

		ExtractTemplatePara extractTemplatePara = ExtractTemplatePara
				.paraseData(data);
		re = compute(extractTemplatePara.getData());
		result.data = re;
		result.state = State.Success;
		return result;
	}

    public byte[] compute(byte[] img) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException{
    	return HisignBVESDK.getTemplateByImageByteArray(img);
    }
	
	public static class ExtractTemplatePara {
        private byte[] imgData;
        
        private THIDFaceSDK.Image decodeImg;
        
        private THIDFaceSDK.Rect rect;
        
        public THIDFaceSDK.Rect getRect() {
            return rect;
        }
        
        public void setRect(THIDFaceSDK.Rect rect) {
            this.rect = rect;
        }
        
       
        
    	public void setDecodeImg(THIDFaceSDK.Image decodeImg) {
    		this.decodeImg = decodeImg;
    	}
    	public THIDFaceSDK.Image getDecodeImg() {
    		return decodeImg;
    	}
        
        public void setData(byte[] data) {
            this.imgData = data;
        }
        public byte[] getData() {
            return imgData;
        }

		public static ExtractTemplatePara paraseData(byte[] para)
				throws ParseParaException {
            
            int imgStartPoint = 4*4; //4个整型的数字来标识人脸框, 全部是零表示不指定人脸框
            
			ExtractTemplatePara extractTemplatePar = new ExtractTemplatePara();
            
            extractTemplatePar.setRect(parseRect(para));
             
            extractTemplatePar.setData(getBytes(para, imgStartPoint, para.length));
			return extractTemplatePar;
        }
        
        public static THIDFaceSDK.Rect parseRect(byte[] para) throws ParseParaException {
            THIDFaceSDK.Rect rect = new THIDFaceSDK.Rect();
            int point = 0;
            rect.left = SystemUtil.byteArrayToInt(getBytes(para, point, point+4));
            point+=4;
            rect.top = SystemUtil.byteArrayToInt(getBytes(para, point, point+4));
            point+=4;
            rect.width = SystemUtil.byteArrayToInt(getBytes(para, point, point+4));
            point+=4;
            rect.height = SystemUtil.byteArrayToInt(getBytes(para, point, point+4));
            
            //4个数字全部为零则返回null
            if (rect.left == rect.top && rect.top == rect.width
                    && rect.width == rect.height && rect.height ==0){
                return null;
            }
            return rect;
        }
    }
}
