package com.hisign.bean;

import com.hisign.util.SystemUtil;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerResultRequest extends Request {

	public int messageType = 3;
	private float score;
	private int status;
	private String statusMessage;
	private String uuid_connId;
	
	static private Logger logger = LoggerFactory.getLogger(WorkerResultRequest.class);
	
	public WorkerResultRequest(){
		
	}

	public void parsePara(byte[] para){
		parsePara(para, 0);
	}
	
	public void parsePara(byte[] para, int skip){
		logger.info("Get result length:" + para.length);
		int point = 0 + skip;
		status = SystemUtil.singleByteToInt(para[point]);
		point += 1;
		
		score = SystemUtil.fourByte2Float(Arrays.copyOfRange(para, point, point+4));
		point += 4;
		
		int idLength = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, point, point+4));
		point += 4;
		uuid_connId = new String(Arrays.copyOfRange(para, point, point+idLength));
		point += idLength;
		
		int messageLength = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, point, point+4));
		point += 4;
		statusMessage = new String(Arrays.copyOfRange(para, point, point+messageLength));
	}
	
	public int getSize(){
		return 1 + 1 + 4 + 4 +uuid_connId.getBytes().length + 4 + statusMessage.getBytes().length;
	}
	
	
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	public String getUuid_connId() {
		return uuid_connId;
	}
	public void setUuid_connId(String uuid_connId) {
		this.uuid_connId = uuid_connId;
	}
}
