package com.hisign.exception;

public class ParseResultException extends  Exception{
	
	private String message;

	public ParseResultException(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
}
