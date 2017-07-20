package com.hisign.constants;

import java.util.UUID;

import com.hisign.bean.ClientRequest;
import com.hisign.util.SystemUtil;

public class SystemConstants {
	
	public static String SDKVersion = "8.3";
	
	public static int MAX_SDK_BATCH = 16;
	
	public static int MaxWorker = 1;
	public static int DECODE_THREAD_COUNT = 4;
	public static int NettyServerPort = 8091;
	public static String NettyServerAddr = "127.0.0.1";
	
	public static String TOKEN = "HISIGN_API_TOKEN";
	
	public static String UTF8 = "UTF-8";
	
	public static String MAGIC = "HBVE";
	public static byte[] MAGIC_BYTES = MAGIC.getBytes();
	public static int CURRENT_VERSION = 1;
	public static byte[] CURRENT_VERSION_BYTES = SystemUtil.int2byte(CURRENT_VERSION);
	
	public static byte[] header;//todo-合并一下
	public static int Header_Len = MAGIC_BYTES.length + CURRENT_VERSION_BYTES.length;
	
	public static int UUIDLength = 32;
	
	public static void main(String[] args) {
	}
}
