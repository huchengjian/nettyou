package com.hisign.netty.worker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.thid.FacePos;
import com.hisign.thid.GrayImg;
import com.hisign.thid.Point;
import com.hisign.thid.THIDFaceSDK;

public class HisignBVESDK {
	
	static String fileName1 = "F:\\1.jpg";
	static String fileName2 = "F:\\2.jpg";
	
	public static void main(String[] args) throws IOException, HisignSDKException, NoFaceDetectException {
		int init = THIDFaceSDK.init(null, null, null);
		System.out.println("init:" + init);
		
		byte[] i1 = getTemplateByImageByteArray(readFile(new File(fileName1)));
		byte[] i2 = getTemplateByImageByteArray(readFile(new File(fileName2)));
		System.out.println(compareFromTwoTemplate(i1, i2));
		
	}
	
	public static float compareFromTwoImages(byte[] img1, byte[] img2) throws HisignSDKException, NoFaceDetectException{
		
		float score = (float) 0.0; 
//		int init = THIDFaceSDK.init(null, null, null);
		
        byte[] template1 = getTemplateByImageByteArray(img1);
        byte[] template2 = getTemplateByImageByteArray(img2);
        
        score = THIDFaceSDK.verify(template1, template2);
        return score;
	}
	
	public static float compareFromTwoTemplate(byte[] temp1, byte[] temp2){
		
//		System.out.println("init value:" +THIDFaceSDK.init(null, null, null));
		
		float score = (float) 0.0; 
		
		score = THIDFaceSDK.verify(temp1, temp2);
		System.out.println("compare score:" + score);
		return score;
	}
	
	public static byte[] readFile(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        byte[] bs = new byte[(int) file.length()];
        dis.readFully(bs);
        dis.close();
        return bs;
    }
	
	/**
	 * 获取模板数据
	 * @param img
	 * @return
	 * @throws NoFaceDetectException 
	 */
	public static byte[] getTemplateByImageByteArray(byte[] img) throws HisignSDKException, NoFaceDetectException{

		int tempsize = THIDFaceSDK.templateSize();
		byte[] template = new byte[tempsize];

		GrayImg grayimg = new GrayImg();
		grayimg = THIDFaceSDK.readJPG(img);
		
		if (grayimg==null) {
			System.out.println("Can't read grayimg, maybe not an image file");
		}
		
		// 获取脸部数据
		List<FacePos> facePoses = new ArrayList<FacePos>();
		int detect = THIDFaceSDK.detect(grayimg, null, null, 1, facePoses);
		System.out.println("detect返回值：" + detect);
		
		if(detect < 0){
			System.out.println("not detect face error");
			throw new NoFaceDetectException();
		}
		
		
		Point[] points = new Point[88];
		int align = 0;
		if (facePoses.size() > 0) {
//			align = THIDFaceSDK.align(grayimg, eyepos, points);
			FacePos facePos = facePoses.get(0);
			align = THIDFaceSDK.alignByFace(grayimg, facePos.rect, points);
		}
		System.out.println("align返回值：" + align);

		// 获取模板数据
		if (facePoses.size() > 0) {
			THIDFaceSDK.extract(grayimg, points, template);
		} else {
			template = null;
		}
		return template;
	}
}
