package com.hisign.netty.worker.handler;

import java.util.Arrays;

import com.hisign.exception.ParseParaException;

public abstract class WorkerHandler {
	
	public static byte getByte(byte[] data, int index) throws ParseParaException {
    	
    	int len = data.length;
    	
    	if (index > len || index < 0) {
			throw new ParseParaException("ComputSimiLarity参数解析错误");
		}
    	else {
			return data[index];
		}
	}
    
	public static byte[] getBytes(byte[] src, int s, int e) throws ParseParaException {
    	int len = src.length;
    	
    	if (s > len || e > len || s < 0 || e < 0) {
    		throw new ParseParaException("ComputSimiLarity参数解析错误");
		}
    	else {
			return Arrays.copyOfRange(src, s, e);
		}
	}
	
}
