package com.hisign.netty.server;

import java.util.HashMap;
import java.util.Map;

public class Metrics {
	
	public static Map<String, Long> lastConnTimeMap;
	
	static{
		lastConnTimeMap = new HashMap<String, Long>();
	}

}
