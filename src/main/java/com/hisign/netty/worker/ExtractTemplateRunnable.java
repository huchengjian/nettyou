package com.hisign.netty.worker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.exception.HisignSDKException;
import com.hisign.exception.NoFaceDetectException;
import com.hisign.exception.ParseParaException;
import com.hisign.netty.worker.handler.ComputeSimilarityHandler;

public class ExtractTemplateRunnable implements Runnable {
	
	static private Logger logger = LoggerFactory.getLogger(ExtractTemplateRunnable.class);
	
	public Task task;
	
	public static class Task{
		
		public byte[] img;
		public byte[] template;
		public int status;
		public boolean isFinish = true;
		public Condition con;
		public Lock lock;
		
		public Task(byte[] img, byte[] template, Condition c, Lock lock){
			this.img = img;
			this.template = template;
			con = c;
			this.lock = lock;
		}
	}
	
	public ExtractTemplateRunnable(Task task){
		this.task = task;
	}

	@Override
	public void run() {
		task.lock.lock();
		try {
			logger.info("Running extract template task.");
			
			task.template = HisignBVESDK.getTemplateByImageByteArray(task.img);
			task.status = 0;
		} catch (HisignSDKException e) {
			task.status = -1;
			task.template = null;
			e.printStackTrace();
		} catch (NoFaceDetectException e) {
			task.status = -2;
			task.template = null;
			e.printStackTrace();
		} catch (ParseParaException e) {
			task.status = -3;
			task.template = null;
			e.printStackTrace();
		} catch (IOException e) {
			task.status = -4;
			task.template = null;
			e.printStackTrace();
		}
		finally{
			logger.info("Task Finish.");
			task.isFinish = true;
			task.con.signal();
			task.lock.unlock();
		}
	}
}
