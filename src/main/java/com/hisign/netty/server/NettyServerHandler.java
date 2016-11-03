package com.hisign.netty.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hisign.hbve.protocol.HBVEBinaryProtocol;
import com.hisign.hbve.protocol.HBVEMessage;
import com.hisign.hbve.protocol.HBVEMessageType;
import com.hisign.util.SHA1;
import com.hisign.util.SystemUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 每个新连接会产生新的NettyServerHandler，会绑定到对应的NIOEventLoop中的线程
 * @author hugo
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	
	private NettyServer server;
	private DelayQueue<HBVEMessage> timeoutQueue;
	
	private Thread timeOutChecker;

	AtomicBoolean isValidate = new AtomicBoolean();
	
    static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	NettyServerHandler(NettyServer server){
		logger.info("Netty server start");
		this.server = server;
		isValidate.set(false);
		
		timeoutQueue = new DelayQueue<HBVEMessage>();
		timeOutChecker = new Thread(new TimeOutChecker(server, timeoutQueue));
//		timeOutChecker.start();
	}
	
	 /**
     * 活跃通道.
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().remoteAddress() + "：Server 通道激活");
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	ctx.close();
        logger.info(ctx.channel().remoteAddress() + "：Server 通道关闭");
    }
    

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

		HBVEMessage request = (HBVEMessage) msg;
		request.ctx = ctx;
		process(request);
		
//      if (!isValidate.get() && !validate(body, ctx)) {
//			return;
//		}
//        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
    }
    
    public void process(HBVEMessage req){
    	System.out.println("log stuff");
    	
    	byte type = req.header.messageType;
    	
    	if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Error)) {
			//error message
		}
    	else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Client)) {
			//client request
    		processClientRequest(req);
		}
    	else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Fetch)){
			//worker fetch task, 总是取最大的version的worker，目前只支持sdk的升级，sdk的降级还未实现
    		//即需要降级时可能会导致所有的client请求失效，解决方法：在maxversion的worker隔一段时间未出现时，将maxversion改成第二大的worker
    		System.out.println("current max version " + NettyServer.maxWorkerVersion + " Worker version " + req.getWorkerSDKVersion());
    		if (req.getWorkerSDKVersion() > NettyServer.maxWorkerVersion.get()) {
    			NettyServer.maxWorkerVersion.set(req.getWorkerSDKVersion());
			}
    		processWorkerRequest(req);
		}
    	else if (HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Result)
                ||
                HBVEMessageType.getMessageType(type).equals(HBVEMessageType.MessageType.Worker_Error_Result)
                ){
			processResult(req);
		}
    }
    
    public void processClientRequest(HBVEMessage clientMes){
    	
    	server.allClientQueue.add(clientMes);
    	logger.info("新增任务，任务数:"+ server.allClientQueue.size());
//		timeoutQueue.add(clientMes);
    	
		wakeupWaitingWorkIfNeed(server.maxWorkerVersion.get());
//		msg.setEndTime(SprocessClientRequestystem.currentTimeMillis() + 5000);
    }
    
	public void wakeupWaitingWorkIfNeed(float version) {
		HBVEMessage waitWorker = null;
		while(true){
			
			if (server.waitingQueue.get(version) == null) {
				logger.info("try to wakeup worker, but no more worker in " + version + " queue! Current max version " + version);
				return;
			}
			
			waitWorker = server.waitingQueue.get(version).poll();
			if (waitWorker == null){
				logger.info("try to wakeup worker, but no more worker in " + version + " queue!");
				return;
			}
			if(waitWorker.ctx != null && waitWorker.ctx.channel().isActive()) {
				logger.info("wakeup worker.");
				break;
			}
		}
		processWorkerRequest(waitWorker); 
	}
    
//    public void fetchTask(HBVEMessage msg){
//		processWorkerRequest(workerRequest);
//    }
    
	/**
	 * worker fetch task
	 * @param worker
	 */
    public void processWorkerRequest(HBVEMessage worker) {

		HBVEMessage clientTask = null;
		
		float version = worker.header.workerSDKVersion;
		
		while(true){
			//worker not the max version, do nothing
			if (version < server.maxWorkerVersion.get()) {
				break;
			}
			
			clientTask = server.allClientQueue.poll();
			if (clientTask == null) {
				break;
			}
			else if (clientTask.isTimeOut) {
				logger.info("processWorkerRequest: connection time out." + clientTask.toString());
				/**
				 * todos 超时后是否需要通知client
				 */
				break;
			}
			else {
				break;
			}
		}
		
		ByteBuf byteBuf = Unpooled.buffer(1024);
		if (clientTask != null) {
			logger.info("worker消费任务, 剩余任务数:" + server.allClientQueue.size());
			
			// 后期需要增加容错机制，consumingChannel为正在处理中的任务，需要对任务长期执行出错的处理
			int connId = clientTask.header.connId;
			String uuidConnId = SystemUtil.encodeConnId(String.valueOf(connId));

			worker.header.uuid = uuidConnId;
			server.consumingChannel.put(uuidConnId, clientTask);
			
			ByteBuf buf = Unpooled.buffer(1 + 32 + clientTask.data.length);
			buf.writeByte(worker.header.messageType | clientTask.header.messageType);
			buf.writeBytes(worker.header.uuid.getBytes());
			buf.writeBytes(clientTask.data);
			
			HBVEBinaryProtocol.writeChannel(worker.ctx, buf.array());
			
		} else {
            logger.info("No task now, pending worker");
			server.addWatiWorker(worker);
			return;
		}
	}

	public void processResult(HBVEMessage workerResult){
		logger.info("完成任务");

//        if ( (workerResult.header.messageType | HBVEMessageType.EXCEPTION) != 0 ){
//
//        }

		String uuidConnId = workerResult.header.uuid;
		HBVEMessage clientMes = server.consumingChannel.get(String.valueOf(uuidConnId));
		if (clientMes == null) {
			logger.error("not found uuidConnId in workqueue:" + uuidConnId);
			return;
		}

        ByteBuf buf = Unpooled.buffer(1 + 4 + workerResult.data.length);
        buf.writeByte(workerResult.header.messageType & (~HBVEMessageType.WORKER_FLAG));
        buf.writeInt(clientMes.header.connId);
        buf.writeBytes(workerResult.data);

        System.out.println("result Len : "+ buf.array().length);

        HBVEBinaryProtocol.writeChannel(clientMes.ctx, buf.array());

		//todo判断任务是否超时

		server.consumingChannel.remove(uuidConnId);
		logger.info("consumingChannel size:" + server.consumingChannel.size());
	}

    
    private boolean validateIdentity(String timestamp, String nonce, String token, String signature){
    	
    	List<String> list = new ArrayList<String>(3) {  
            private static final long serialVersionUID = 2621444383666420433L;  
            public String toString() {  
                return this.get(0) + this.get(1) + this.get(2);  
            }  
        };
        
        list.add(token);  
        list.add(timestamp);  
        list.add(nonce);  
        Collections.sort(list);

		String localSign = SHA1.sha1(list.toString().getBytes());

//		logger.info(timestamp+ " "+nonce + " "+ signature+ " " + localSign);
    	
    	return localSign.equals(signature) ? true:false;
    }
    
//    @Override
//    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
////        logger.info("server channelReadComplete..");
//        ctx.flush();//刷新后才将数据发出到SocketChannel
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("server exceptionCaught..");
		logger.error(cause.getMessage());
		cause.printStackTrace();
//		returnStandardMessageToClient(Status.ServerError,
//				SystemUtil.addNewLine(Status.ServerErrorMessg), null, ctx);
    }
}