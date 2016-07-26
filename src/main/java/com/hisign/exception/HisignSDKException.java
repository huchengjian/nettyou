package com.hisign.exception;

public class HisignSDKException extends  Exception{
	
	private String message;

	public HisignSDKException(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
}
