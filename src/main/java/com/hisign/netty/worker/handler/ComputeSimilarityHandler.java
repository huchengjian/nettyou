package com.hisign.netty.worker.handler;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.HisignBVESDK;
import com.hisign.util.SystemUtil;

/**
 * Created by hugo on 8/17/16.
 */
public class ComputeSimilarityHandler extends WorkerHandler{
	
	static private Logger logger = LoggerFactory.getLogger(ComputeSimilarityHandler.class);
	
	public byte[] run(byte[] data) throws HisignSDKException, IOException, ParseParaException {
		ComputeSimilarityPara computeSimilarityPara = ComputeSimilarityPara.paraseData(data);
		float score = compute(computeSimilarityPara.getType1(), computeSimilarityPara.getType2(),
				computeSimilarityPara.getFace1(), computeSimilarityPara.getFace2());
		return SystemUtil.float2byte(score);
	}
	
	private float compute(int type1, int type2, byte[] face1, byte[] face2) throws HisignSDKException, IOException{

    	logger.info("compute similarity!");

		byte[] temp1 = face1, temp2 = face2;
    	if (type1 == 1) {
    		//图片數據
			temp1 = HisignBVESDK.getTemplateByImageByteArray(face1);
        }
    	if (type2 == 1) {
    		//图片數據
			temp2 = HisignBVESDK.getTemplateByImageByteArray(face2);
        }
    	
    	logger.info("size of template:"+temp1.length + " "+temp2.length);
    	return HisignBVESDK.compareFromTwoTemplate(temp1, temp2);
    }


    public static class ComputeSimilarityPara {
        public int type1;
        public int type2;
        public byte[] face1;
        public byte[] face2;
        
        public int getType1() {
    		return type1;
    	}
    	public void setType1(int type1) {
    		this.type1 = type1;
    	}
    	public int getType2() {
    		return type2;
    	}
    	public void setType2(int type2) {
    		this.type2 = type2;
    	}
    	public byte[] getFace1() {
    		return face1;
    	}
    	public void setFace1(byte[] face1) {
    		this.face1 = face1;
    	}
    	public byte[] getFace2() {
    		return face2;
    	}
    	public void setFace2(byte[] face2) {
    		this.face2 = face2;
    	}

        public static ComputeSimilarityPara paraseData(byte[] para) throws ParseParaException {
        	
        	ComputeSimilarityPara computeSimilarityPara = new ComputeSimilarityPara();
        	int point = 0;
        	
        	computeSimilarityPara.setType1(getByte(para, point));
        	point++;
        	computeSimilarityPara.setType2(getByte(para, point));
        	point++;
        	
        	int face1Length = SystemUtil.fourByteArrayToInt(getBytes(para, point, point+4));
        	point += 4;
        	computeSimilarityPara.setFace1(getBytes(para, point, point+face1Length));
        	point += face1Length;
        	
        	int face2Length = SystemUtil.fourByteArrayToInt(getBytes(para, point, point+4));
        	point += 4;
        	computeSimilarityPara.setFace2(getBytes(para, point, point+face2Length));
        	
            return computeSimilarityPara;
        }
    }
}
