package com.hisign.hbve.protocol;

public class HBVEHeader {
	
	public byte messageType;
	public int connId;
	
	public String uuid;
	
	public String workerSDKVersion;
	
	public HBVEHeader(byte t, int id){
		messageType = t;
		connId = id;
	}
	
	public HBVEHeader(byte t, String version, int id){
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
