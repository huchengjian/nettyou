package com.hisign.bean;

public class WorkerRequest extends Request {
	
	public String uuid_connId;
	public byte[] uuidBytes;
	public ClientRequest clientRequest;
	
	public WorkerRequest(){
		clientRequest = new ClientRequest();
	}
	
	/**
	 * messagetype + id_size + id + t1 + t2 + f1_size + f2_size + f1 + f2
	 * @return
	 */
	public int getSize(){
		return 1+4+uuid_connId.length()+
				1+1+4+4
				+clientRequest.getFace1().length
				+clientRequest.getFace2().length;
	}

}
