package com.hisign.netty.worker.handler;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignBVESDK;
import com.hisign.netty.worker.SDKResult;
import com.hisign.netty.worker.SDKResult.State;

public class ExtractTemplateHandler extends WorkerHandler {
	
	public SDKResult run(byte[] data){
		SDKResult result = new SDKResult();
		byte[] re = null;
		
		try {
			ExtractTemplatePara extractTemplatePara = ExtractTemplatePara.paraseData(data);
			re = compute(extractTemplatePara.getData());
			result.data = re;
			result.state = State.Success;
		} catch (NoFaceDetectException e) {
			e.printStackTrace();
			result.state= State.NotDetectFace;
		} catch (HisignSDKException e) {
			e.printStackTrace();
			result.state= State.SDKError;
		} catch (ParseParaException e) {
			e.printStackTrace();
			result.state= State.ParameterError;
		}
		return result;
	}

    public byte[] compute(byte[] img) throws HisignSDKException, NoFaceDetectException{
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
