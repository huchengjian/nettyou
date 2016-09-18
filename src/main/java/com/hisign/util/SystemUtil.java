package com.hisign.util;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.hisign.constants.SystemConstants;

public class SystemUtil {
	
	public static String encodeConnId(String connid) {
		UUID randomUuid = UUID.randomUUID();
		return connid.concat(randomUuid.toString().replace("-", "")).substring(0, 32);
	}

//	public static String getUUID(String connid){
//		return UUID.randomUUID();
//	}
	
	public static byte[] int2byte(int res) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (res >>> (24 - i * 8));
		}
		return b;
	}
	
	public static int decodeConnId(String uuid) {
		if (uuid.length() > SystemConstants.UUIDLength) {
			return Integer.parseInt(uuid.substring(SystemConstants.UUIDLength));
		}
		return 0;
	}

	public static float byte2float(byte[] b, int index) {
		int l;
		l = b[index + 0];
		l &= 0xff;
		l |= ((long) b[index + 1] << 8);
		l &= 0xffff;
		l |= ((long) b[index + 2] << 16);
		l &= 0xffffff;
		l |= ((long) b[index + 3] << 24);
		return Float.intBitsToFloat(l);
	}
	
	public static byte [] long2ByteArray (long value)
	{
	    return ByteBuffer.allocate(8).putLong(value).array();
	}

	public static byte[] float2byte(float f) {

		// 把float转换为byte[]
		int fbit = Float.floatToIntBits(f);

		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (fbit >> (24 - i * 8));
		}

		// 翻转数组
		int len = b.length;
		// 建立一个与源数组元素类型相同的数组
		byte[] dest = new byte[len];
		// 为了防止修改源数组，将源数组拷贝一份副本
		System.arraycopy(b, 0, dest, 0, len);
		byte temp;
		// 将顺位第i个与倒数第i个交换
		for (int i = 0; i < len / 2; ++i) {
			temp = dest[i];
			dest[i] = dest[len - i - 1];
			dest[len - i - 1] = temp;
		}

		return dest;

	}
	
	public static byte bit2byte(String bString) {
		byte result = 0;
		for (int i = bString.length() - 1, j = 0; i >= 0; i--, j++) {
			result += (Byte.parseByte(bString.charAt(i) + "") * Math.pow(2, j));
		}
		return result;
	}
	
	public static int fourByteArrayToInt(byte[] b) {  
	    return   b[3] & 0xFF |  
	            (b[2] & 0xFF) << 8 |  
	            (b[1] & 0xFF) << 16 |  
	            (b[0] & 0xFF) << 24;  
	}

	public static float fourByte2Float(byte[] b) {
		int l;
		int index = 0;
		l = b[index + 0];
		l &= 0xff;
		l |= ((long) b[index + 1] << 8);
		l &= 0xffff;
		l |= ((long) b[index + 2] << 16);
		l &= 0xffffff;
		l |= ((long) b[index + 3] << 24);
		return Float.intBitsToFloat(l);
	}

	public static int singleByteToInt(byte b) {  
	    return  b;  
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
	
	public static void main(String[] args) {
		System.out.println( byte2float( float2byte((float)0.783242), 0));
		System.out.println(bytesToHexString(int2byte(1)));
		Integer s;
		int x = Float.floatToIntBits((float) 9.375);
		
		System.out.println(bytesToHexString(int2byte(x)));
	}
}
