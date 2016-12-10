package com.hisign.netty.worker;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

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
	
	static String fileName1 = "/Users/hugo/test/e2.jpg";
	static String fileName2 = "/Users/hugo/Documents/8079";
	
	static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	public static void main(String[] args) throws IOException, HisignSDKException, NoFaceDetectException, ParseParaException {
//		int init = THIDFaceSDK.init(null, null, null);
//		logger.info("init:" + init);
		
//		byte[] i1 = getTemplateByImageByteArray(readFile(new File(fileName1)));
//		byte[] i2 = getTemplateByImageByteArray(readFile(new File(fileName2)));
		
		System.out.println(readImg(new File(fileName1))==null);
		
	}
	
	public static float compareFromTwoImages(byte[] img1, byte[] img2) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException{
		
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
	 * @throws IOException 
	 */
	public static byte[] getTemplateByImageByteArray(byte[] img) throws HisignSDKException, NoFaceDetectException, ParseParaException, IOException{
		
		int tempsize = THIDFaceSDK.templateSize();
		byte[] template = new byte[tempsize];

		GrayImg grayimg;
		grayimg = THIDFaceSDK.readJPG(img);
		if (grayimg == null) {
			logger.info("Error: THIDFaceSDK readJPG error, grayimg is null, use java code retry.");
			//java read重试
			grayimg = readImg(img);
		}
		
		if (grayimg == null) {
			logger.info("Error: Can't read grayimg, maybe not an image file");
			throw new ParseParaException("Can't read grayimg, maybe not an image file");
		}
		
		// 获取脸部数据
		List<FacePos> facePoses = new ArrayList<FacePos>();
		int detect = THIDFaceSDK.detect(grayimg, null, null, 1, facePoses);
		logger.info("detect返回值：" + detect);
		
		if(detect < 0){
			logger.info("Error: Not detect face.");
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
			int x = THIDFaceSDK.extract(grayimg, points, template);
			logger.info("extract:" + x);
		} else {
			template = null;
		}
		
		if (template == null || template.length == 0) {
			logger.info("Error: template size is zero");
			throw new ParseParaException("template size is zero");
		}
		return template;
	}
	
	public static GrayImg readImg(InputStream stream) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        if (img == null) return null;
        GrayImg gimg = new GrayImg();
        gimg.width = img.getWidth();
        gimg.height = img.getHeight();
        if (img.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            gimg.raw = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        } else {
            gimg.raw = new byte[gimg.width * gimg.height];
            for (int x = 0; x < gimg.width; x++) {
                for (int y = 0; y < gimg.height; y++) {
                    int rgb = img.getRGB(x, y);
                    int R = (rgb >>> 16) & 0xff;
                    int G = (rgb >>> 8) & 0xff;
                    int B = (rgb) & 0xff;;
                    gimg.raw[y * gimg.width + x] = (byte) ((R * 299 + G * 587 + B * 114 + 500) / 1000);
                }
            }
        }
        return gimg;
    }

    public static GrayImg readImg(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            return readImg(fis);
        } finally {
            fis.close();
        }
    }

    public static GrayImg readImg(byte[] data) throws IOException {
        return readImg(new ByteArrayInputStream(data));
    }
    
    public static byte[] readFile(String filename) throws IOException {
		
		File file = new File(filename);
		
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        byte[] bs = new byte[(int) file.length()];
        dis.readFully(bs);
        dis.close();
        
        return bs;
    }
}
