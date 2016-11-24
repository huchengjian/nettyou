package com.hisign.netty.worker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.server.NettyServerHandler;
import com.hisign.thid.FacePos;
import com.hisign.thid.GrayImg;
import com.hisign.thid.Point;
import com.hisign.thid.THIDFaceSDK;

public class HisignBVESDK {
	
	static String fileName1 = "F:\\1.jpg";
	static String fileName2 = "F:\\2.jpg";
	
	static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	public static void main(String[] args) throws IOException, HisignSDKException, NoFaceDetectException, ParseParaException {
		int init = THIDFaceSDK.init(null, null, null);
		logger.info("init:" + init);
		
		byte[] i1 = getTemplateByImageByteArray(readFile(new File(fileName1)));
		byte[] i2 = getTemplateByImageByteArray(readFile(new File(fileName2)));
		
	}
	
	public static float compareFromTwoImages(byte[] img1, byte[] img2) throws HisignSDKException, NoFaceDetectException, ParseParaException{
		
		float score = (float) 0.0; 
//		int init = THIDFaceSDK.init(null, null, null);
		
        byte[] template1 = getTemplateByImageByteArray(img1);
        byte[] template2 = getTemplateByImageByteArray(img2);
        
        score = THIDFaceSDK.verify(template1, template2);
        return score;
	}
	
	public static float compareFromTwoTemplate(byte[] temp1, byte[] temp2){
		
		float score = (float) 0.0; 
		
		score = THIDFaceSDK.verify(temp1, temp2);
		logger.info("compare score:" + score);
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
	 * @throws ParseParaException 
	 */
	public static byte[] getTemplateByImageByteArray(byte[] img) throws HisignSDKException, NoFaceDetectException, ParseParaException{

		int tempsize = THIDFaceSDK.templateSize();
		byte[] template = new byte[tempsize];

		GrayImg grayimg = new GrayImg();
		grayimg = THIDFaceSDK.readJPG(img);
		
		if (grayimg==null) {
			logger.info("Can't read grayimg, maybe not an image file");
			throw new ParseParaException("Can't read grayimg, maybe not an image file");
		}
		
		// 获取脸部数据
		List<FacePos> facePoses = new ArrayList<FacePos>();
		int detect = THIDFaceSDK.detect(grayimg, null, null, 1, facePoses);
		logger.info("detect返回值：" + detect);
		
		if(detect < 0){
			logger.info("not detect face error");
			throw new NoFaceDetectException();
		}
		
		
		Point[] points = new Point[88];
		int align = 0;
		if (facePoses.size() > 0) {
//			align = THIDFaceSDK.align(grayimg, eyepos, points);
			FacePos facePos = facePoses.get(0);
			align = THIDFaceSDK.alignByFace(grayimg, facePos.rect, points);
		}
		logger.info("align返回值：" + align);

		// 获取模板数据
		if (facePoses.size() > 0) {
			THIDFaceSDK.extract(grayimg, points, template);
		} else {
			template = null;
		}
		
		if (template == null || template.length == 0) {
			logger.info("Can't read grayimg, maybe not an image file");
			throw new ParseParaException("Can't read grayimg, maybe not an image file");
		}
		return template;
	}
}
