package com.hisign.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParseContext;
import com.hisign.netty.server.NettyServer;
import com.hisign.util.SystemUtil;

public class ClientRequest extends Request {
	
	static private Logger logger = LoggerFactory.getLogger(ClientRequest.class);
	
	private int conn_id;
	private int type1;
	private int type2;
	private byte[] face1;
	private byte[] face2;
	
	public long timestamp;
	
	private boolean isTimeOut = false;
	
	public ClientRequest(){
		timestamp = System.currentTimeMillis();
	}
	
	public ClientRequest(int conn_id, int message_type, int type1, int type2,
			byte[] face1, byte[] face2) {
		super();
		this.message_type = message_type;
		this.conn_id = conn_id;
		this.type1 = type1;
		this.type2 = type2;
		this.face1 = face1;
		this.face2 = face2;
	}
	
	public void parseParameter(){
		parseParameter(para, 0);
	}
	
	public void parseParameter(byte[] para, int skip){
		
//		message_type = SystemUtil.singleByteToInt(para[0]);
		
		conn_id = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, 1, 5));
		type1 = SystemUtil.singleByteToInt(para[5]);
		type2 = SystemUtil.singleByteToInt(para[6]);
		
		int face1_length = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, 7, 11));
		int face1_end = face1_length + 11;
		face1 = Arrays.copyOfRange(para, 11, face1_end);
		
		int face2_length = SystemUtil.fourByteArrayToInt(Arrays.copyOfRange(para, 7, 11));
		face2 = Arrays.copyOfRange(para, face1_end+4, face1_end+4 + face2_length);
		
	}
	
	@Override
	public String toString() {
		logger.info(message_type + " " + conn_id + " " + 
				type1 + " " + type2 + " " + SystemUtil.bytesToHexString(face1) + " "
				+ SystemUtil.bytesToHexString(face2));
		
		return super.toString();
	}
	
	/**
	 * 写回结果到客户端
	 * @param workerResultRequest
	 * @param conn
	 */
	public void writeResultToClient(WorkerResultRequest workerResultRequest){
		
		ClientResult clientResult = new ClientResult(
				workerResultRequest.getStatus(),
				workerResultRequest.getStatusMessage(), 
				SystemUtil.decodeConnId(workerResultRequest.getUuid_connId()), 
				workerResultRequest.getScore()
				);
		
        ByteBuf resultBytebuf = Unpooled.buffer(1024);
        resultBytebuf.writeInt(clientResult.getSize());
        
        resultBytebuf.writeByte((byte)clientResult.getStatus());
        resultBytebuf.writeFloat(clientResult.getScore());
        resultBytebuf.writeInt(clientResult.getConn_id());
        resultBytebuf.writeInt(clientResult.getStatusMessageBytes().length);
        resultBytebuf.writeBytes(clientResult.getStatusMessageBytes());
        
        getChannelHandlerContext().writeAndFlush(resultBytebuf);
	}

	
	public int getConn_id() {
		return conn_id;
	}
	public void setConn_id(int conn_id) {
		this.conn_id = conn_id;
	}
	public int getType1() {
		return type1;
	}
	public void setType1(int type1) {
		this.type1 = type1;
	}
	public int getType2() {
		return type2;
	}
	public void setType2(int type2) {
		this.type2 = type2;
	}
	public byte[] getFace1() {
		return face1;
	}
	public void setFace1(byte[] face1) {
		this.face1 = face1;
	}
	public byte[] getFace2() {
		return face2;
	}
	public void setFace2(byte[] face2) {
		this.face2 = face2;
	}

	public int compareTo(Delayed o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void setIsTimeOut(){
		isTimeOut = true;
	}
	
	public boolean getIsTimeOut(){
		return isTimeOut;
	}

	public long getDelay(TimeUnit unit) {
		// TODO Auto-generated method stub
		return 5000;
	}
}
