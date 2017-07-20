package com.hisign.hbve.protocol;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.hisign.netty.worker.handler.ComputeSimilarityHandler;
import com.hisign.netty.worker.handler.ExtractTemplateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;

public class HBVEMessage implements Delayed{
	
	static private Logger logger = LoggerFactory.getLogger(HBVEMessage.class);

    public HBVEHeader header;
    public byte[] data;
    
    public ChannelHandlerContext ctx = null;
    
    public ComputeSimilarityHandler.ComputeSimilarityPara computeSimilarityPara;
    public ExtractTemplateHandler.ExtractTemplatePara extractTemplatePara;

    public long start;
    public long timeout;//超时时间 单位纳秒

    public boolean isTimeOut = false;

    public float getWorkerSDKVersion(){
    	return header.workerSDKVersion;
    }

    public HBVEMessage(HBVEHeader h, byte[] d) {
        header = h;
        data = d;
        start = System.nanoTime();
    }

    public void print() {
    	logger.info("type + dataLen:" + (int)header.messageType + " " + data.length);
    }

    public int compareTo(Delayed o) {
		return 0;
	}

    public long getDelay(TimeUnit unit) {
        long d = unit.convert(timeout - runningTime(), TimeUnit.NANOSECONDS);
        return d;
    }
    
    /**
     * 运行时间，单位纳秒
     * @return
     */
    private long runningTime() {
        return System.nanoTime() - start;
    }

	public void setIsTimeOut(){
		logger.info("Message timeout.");
        isTimeOut = true;
    }
    
    
}
