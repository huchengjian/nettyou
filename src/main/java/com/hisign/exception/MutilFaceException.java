package com.hisign.exception;

public class MutilFaceException extends  Exception{
	
	private static final long serialVersionUID = 9186181015692655002L;
	
	public final static byte errorId = 0x02;
	public final static String errorMessage = "Detect mutiple faces， please check your image.";
	
	private String message;
	
	private int faceCount;

	public MutilFaceException(String message, int faceCount){
		this.message = message;
		this.faceCount = faceCount;
	}
	
	public MutilFaceException(int faceCount){
		this.message = errorMessage;
		this.faceCount = faceCount;
	}
	
	public String getMessage(){
		return this.message;
	}
	
	public int getFaceCount(){
		return this.faceCount;
	}
}

