package com.hisign.exception;

public class HisignSDKException extends  Exception{
	
	private static final long serialVersionUID = -6642182893030876580L;
	
	public final static byte errorId = 0x01;
	public final static String DefaultErrorMessage = "SDK compute error， please check your parameter.";
	
	private String message;

	public HisignSDKException(String message){
		this.message = message;
	}
	
	public HisignSDKException(){
		this.message = DefaultErrorMessage;
	}
	
	public String getMessage(){
		return this.message;
	}
}
