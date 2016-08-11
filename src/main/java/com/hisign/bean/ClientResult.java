package com.hisign.bean;

import java.io.UnsupportedEncodingException;

/**
 * 客户端返回值数据结构
 * @author hugo
 */
public class ClientResult {
	
	private int status;
	private String statusMessage;
	private byte[] statusMessageBytes;
	private int conn_id;
	private float score;
	
	public ClientResult(int status, String statusMessage, int conn_id,
			float score) {
		super();
		this.status = status;
		this.statusMessage = statusMessage;
		this.conn_id = conn_id;
		this.score = score;
		try {
			statusMessageBytes = statusMessage.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getSize() {
		return 1+4+4+4+statusMessageBytes.length;
	}
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	public int getConn_id() {
		return conn_id;
	}
	public void setConn_id(int conn_id) {
		this.conn_id = conn_id;
	}
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}

	public byte[] getStatusMessageBytes() {
		return statusMessageBytes;
	}

	public void setStatusMessageBytes(byte[] statusMessageBytes) {
		this.statusMessageBytes = statusMessageBytes;
	}
}
