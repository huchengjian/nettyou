package com.hisign.exception;

public class ConfigException extends  Exception{
	
	private static final long serialVersionUID = -6642182893030876580L;
	
	public final static byte errorId = 0x01;
	public final static String DefaultErrorMessage = "ConfigException， please check your config.ini.";
	
	private String message;

	public ConfigException(String message){
		this.message = message;
	}
	
	public ConfigException(){
		this.message = DefaultErrorMessage;
	}
	
	public String getMessage(){
		return this.message;
	}
}
