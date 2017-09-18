package com.hisign.hbve.protocol;

public class HBVEHeader {
	
	public byte messageType;
	public int connId;
	public int faceCount; //需要检测的人脸数量的参数, 若为零则标识选取最大的人脸
	
	public String uuid;
	
	public float workerSDKVersion;
	
	public HBVEHeader(byte t, int id){
		messageType = t;
		connId = id;
	}
	
	public HBVEHeader(byte t, float version, int id){
		messageType = t;
		workerSDKVersion = version;
		connId = id;
	}
	
	public HBVEHeader(byte t, String u){
		messageType = t;
		uuid = u;
	}
	
	public HBVEHeader(byte t){
		messageType = t;
	}
}
