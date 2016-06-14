package com.hisign.netty.worker;

import java.util.ArrayList;
import java.util.List;

import com.hisign.thid.EyePos;
import com.hisign.thid.FacePos;
import com.hisign.thid.GrayImg;
import com.hisign.thid.Point;
import com.hisign.thid.THIDFaceSDK;

public class HisignBVESDK {
	
	public static float compareFromTwoImages(byte[] img1, byte[] img2){
		
		float score = (float) 0.0; 
		int init = THIDFaceSDK.init(null, null, null);
		
        byte[] template1 = getTemplateByImageByteArray(img1);
        byte[] template2 = getTemplateByImageByteArray(img2);
        
        score = THIDFaceSDK.verify(template1, template2);
        
        return score;
	}
	
	public static float compareFromTwoTemplate(byte[] temp1, byte[] temp2){
		
		float score = (float) 0.0; 
		int init = THIDFaceSDK.init(null, null, null);
		
		score = THIDFaceSDK.verify(temp1, temp2);
		return score;
	}
	
	
	public static byte[] getTemplateByImageByteArray(byte[] img) {
		
		GrayImg grayimg = new GrayImg();
		grayimg = THIDFaceSDK.readJPG(img);
		
		int tempsize = THIDFaceSDK.templateSize();
		byte[] template = new byte[tempsize];
		
		//获取脸部数据
		List<FacePos> facePoses = new ArrayList<FacePos>();
        int detect = THIDFaceSDK.detect(grayimg, null, null, 1, facePoses);
        System.out.println("detect返回值：" + detect);
        EyePos eyepos = new EyePos();
        int locate = 0;
        if (facePoses.size() > 0) {
        	locate = THIDFaceSDK.locate(grayimg, facePoses.get(0).rect, eyepos);
        }
        System.out.println("locate返回值：" + locate);
        Point[] points = new Point[88];
        int align = 0;
        if (facePoses.size() > 0 && eyepos.left != null & eyepos.right != null) {
           align = THIDFaceSDK.align(grayimg, eyepos, points);
        }
        System.out.println("align返回值：" + align);
        
        //获取模板数据
        if (facePoses.size() > 0 && eyepos.left != null && eyepos.right != null) {
            THIDFaceSDK.extract(grayimg, points, template);
        } else{
        	template = null;
        }
        
        return template;

	}
}
