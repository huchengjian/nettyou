package com.hisign.test;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tester {
	
	public static void main(String[] args) {  
        Map<String, String> map = new LinkedHashMap<String, String>(16, 0.75f, true);  
        for (int i = 0; i < 10; i++) {  
            map.put("key" + i, "value" + i);  
        }  
        
        map.get("key" + 3); 
        map.get("key" + 5); 
        
        for (String value : map.keySet()) {  
            System.out.println(value);  
        }  
    } 

}
