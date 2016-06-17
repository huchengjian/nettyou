package com.hisign.util;

public class SystemUtil {
	
	public static byte bit2byte(String bString) {
		byte result = 0;
		for (int i = bString.length() - 1, j = 0; i >= 0; i--, j++) {
			result += (Byte.parseByte(bString.charAt(i) + "") * Math.pow(2, j));
		}
		return result;
	}
	
	private static int parse(char c) {  
	    if (c >= 'a')  
	        return (c - 'a' + 10) & 0x0f;  
	    if (c >= 'A')  
	        return (c - 'A' + 10) & 0x0f;  
	    return (c - '0') & 0x0f;  
	} 
	
	public static String addNewLine(String str){
		return str + "\n";
		
	}
	
	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}
	
	 /// <summary>
    /// 十六进制字符串转换成字节数组 
    /// </summary>
    /// <param name="hexString">要转换的字符串</param>
    /// <returns></returns>
	public static byte[] HexString2Bytes(String hexstr) {  
	    byte[] b = new byte[hexstr.length() / 2];  
	    int j = 0;  
	    for (int i = 0; i < b.length; i++) {  
	        char c0 = hexstr.charAt(j++);  
	        char c1 = hexstr.charAt(j++);  
	        b[i] = (byte) ((parse(c0) << 4) | parse(c1));  
	    }  
	    return b;  
	}  
}
