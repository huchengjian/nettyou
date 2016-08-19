package com.hisign.netty.worker.handler;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignBVESDK;

public class ExtractTemplateHandler extends WorkerHandler {
	
	public byte[] run(byte[] data) throws ParseParaException, HisignSDKException{
		ExtractTemplatePara extractTemplatePara = ExtractTemplatePara.paraseData(data);
		return compute(extractTemplatePara.getData());
	}

    public byte[] compute(byte[] img) throws HisignSDKException{
    	return HisignBVESDK.getTemplateByImageByteArray(img);
    }
	
	public static class ExtractTemplatePara {
        private byte[] imgData;
        
    	public void setData(byte[] data) {
    		this.imgData = data;
    	}
    	public byte[] getData() {
    		return imgData;
    	}

        public static ExtractTemplatePara paraseData(byte[] para) throws ParseParaException {
        	ExtractTemplatePara extractTemplatePar = new ExtractTemplatePara();
        	extractTemplatePar.setData(para);
        	return extractTemplatePar;
        }
    }
}
