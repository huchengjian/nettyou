package com.hisign.hbve.protocol;

/**
 * 单字节标识的消息状态码，1-6位标识客户端请求类型，
 * 当第七位为标识请求来自客户端or worker，为1时是worker，为0是client。
 * @author hugo
 */
public class HBVEMessageType{
	
	public enum MessageType {
	    Error, Worker_Fetch, Worker_Result, Client;
	}
	
	public enum ClientMessageType {
	    Similarity, Extract_Template;
	}
	
	public static final byte EXCEPTION = (byte) 0x80;
	public static final byte CLIENT_COMPUTE_SIMILARITY = 0x01;
	public static final byte CLIENT_EXTRACT_TEMPLATE = 0x02;
	
	public static final byte WORKER_FLAG = 0x40;
	public static final byte CLIENT_FLAG = 0x3F;
	
	public static MessageType getMessageType(byte type) {
		
		MessageType messageType = null;
		
		if ( (type & HBVEMessageType.EXCEPTION) != 0 ) {
			messageType = MessageType.Error;
		}
    	else if ((type & HBVEMessageType.WORKER_FLAG) == 0 ) {
    		messageType = MessageType.Client;
		}
    	else if ((type & (~HBVEMessageType.WORKER_FLAG) ) == 0 ){
    		messageType = MessageType.Worker_Fetch;
		}
    	else {
    		messageType = MessageType.Worker_Result;
		}
		return messageType;
	}
	
	public static boolean isWorkerMess(byte type){
		return (type & HBVEMessageType.WORKER_FLAG) != 0 ? true : false;
	}
	
	public static ClientMessageType getClientMessageType(byte type){
		byte clientTransform = (byte) (type & CLIENT_FLAG);
		if (clientTransform == CLIENT_COMPUTE_SIMILARITY) {
			return ClientMessageType.Similarity;
		}
		// Todo add MessageType
		else {
			return null;
		}
	}
}
