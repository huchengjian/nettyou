package com.hisign.netty.worker.handler;

import java.io.IOException;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.MutilFaceException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignBVESDK;
import com.hisign.netty.worker.SDKResult;
import com.hisign.netty.worker.SDKResult.State;

public class ExtractTemplateHandler extends WorkerHandler {
	
	public SDKResult run(byte[] data) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException, MutilFaceException{
		SDKResult result = new SDKResult();
		byte[] re = null;

		ExtractTemplatePara extractTemplatePara = ExtractTemplatePara
				.paraseData(data);
		re = compute(extractTemplatePara.getData());
		result.data = re;
		result.state = State.Success;
		return result;
	}

    public byte[] compute(byte[] img) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException, MutilFaceException{
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

		public static ExtractTemplatePara paraseData(byte[] para)
				throws ParseParaException {
			ExtractTemplatePara extractTemplatePar = new ExtractTemplatePara();
			extractTemplatePar.setData(para);
			return extractTemplatePar;
        }
    }
}
