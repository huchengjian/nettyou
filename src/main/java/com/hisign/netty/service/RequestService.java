package com.hisign.netty.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;
import com.hisign.constants.SystemConstants;
import com.hisign.util.SHA1;

public class RequestService {
	
	
	/**
	 * 没有synchronized会导致数据出错，后期可考虑优化
	 * @param jo
	 */
	public synchronized static void addValidateFields(JSONObject jo){
		
		long timeStamp = System.currentTimeMillis();
		int nonce = new Random().nextInt(10000);
		String token = SystemConstants.TOKEN;
		
		List<String> list = new ArrayList<String>(3) {  
            private static final long serialVersionUID = 2621444383666420433L;  
            public String toString() {  
                return this.get(0) + this.get(1) + this.get(2);  
            }  
        };
        
        list.add(token);  
        list.add(String.valueOf(timeStamp));  
        list.add(String.valueOf(nonce));  
        Collections.sort(list);
        
        String sig = SHA1.getDigestOfString(list.toString().getBytes());
        jo.put(Message.Signature, sig);
        jo.put(Message.Nonce, nonce);
        jo.put(Message.Timestamp, timeStamp);
	}
	
	public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
		addValidateFields(new JSONObject());
		System.out.println(System.currentTimeMillis());
	}
}
