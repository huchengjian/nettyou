package com.hisign.exception;

public class ParseParaException extends  Exception{
	
	private static final long serialVersionUID = 9186181015692655003L;
	
	public final static byte errorId = 0x02;
	public final static String errorMessage = "parse parameter error， please check your parameter.";
	
	private String message;

	public ParseParaException(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
}
