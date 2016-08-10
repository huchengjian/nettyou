package com.hisign.netty.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;
import com.hisign.constants.Status;
import com.hisign.constants.SystemConstants;
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
	private DelayQueue<Connection> timeoutQueue;
	
	private Thread timeOutChecker;

	AtomicBoolean isValidate = new AtomicBoolean();
	
    static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	NettyServerHandler(NettyServer server){
		logger.info("Netty server start");
		this.server = server;
		isValidate.set(false);
		
		timeoutQueue = new DelayQueue<Connection>();
		timeOutChecker = new Thread(new TimeOutChecker(server, timeoutQueue));
		timeOutChecker.start();
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
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
       
        String body = new String(req, "UTF-8");

        if (!isValidate.get() && !validate(body, ctx)) {
			return;
		}

        printMess(body);
        Connection connection = new Connection(ctx, body);
        messageProcess(connection);
        
//        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(
//                System.currentTimeMillis()).toString() : "BAD ORDER";
//        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
//        ctx.write(resp);
    }
    
    private void printMess(String body) {
    	String mess = body.length() > 200 ?
    			body.substring(0, 200) : body;
    	
    	logger.info("Master Server Receive Message." + "fulllength:"+ body.length() + "\n" + mess);
	}
    
    private void testcode(String body) {
    	JSONObject jb = JSON.parseObject(body);
    	String dataString = jb.getString(Message.DATA);
    	
    	JSONObject dataJson = JSON.parseObject(dataString);
    	System.out.println(dataJson.getString(Message.Verify1).length());
    	System.out.println(dataJson.getString(Message.Verify2).length());
	}
    
    private boolean validate(String messBody, ChannelHandlerContext ctx){
    	JSONObject para = JSON.parseObject(messBody);
        boolean isValidatedPara = validateParameter(para);
        if (!isValidatedPara) {
        	logger.info("Parameter error. Return back to client.");
        	returnStandardMessageToClient(Status.ParaError, Status.ParaErrorMessg, null, ctx);
        	return false;
		}
        
        String timestamp = para.getString(Message.Timestamp);
    	String nonce = para.getString(Message.Nonce);
    	String signature = para.getString(Message.Signature);
        boolean isValidateIdentity = validateIdentity(timestamp, nonce, SystemConstants.TOKEN, signature);
        
        if (!isValidateIdentity) {
        	logger.info("ValidateIdentity error. Return back to client.");

			returnStandardMessageToClient(Status.IdentityCheckError, Status.IdentityCheckErrorMessg, null, ctx);
        	return false;
		}
		isValidate.set(true);
        return true;
    }
    
    /**
     * 验证参数是否存在
     * @param para json格式的输入
     * @return
     */
    private boolean validateParameter(JSONObject para){
    	
    	Integer type = para.getInteger(Message.MessageType);
    	String timestamp = para.getString(Message.Timestamp);
    	String nonce = para.getString(Message.Nonce);
    	String signature = para.getString(Message.Signature);
    	
    	if (type == null || timestamp == null || nonce == null || signature == null) {
    		return false;
		}
    	return true;
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

//		logger.info("server list:"+list.toString());
		String localSign = SHA1.sha1(list.toString().getBytes());

//		logger.info(timestamp+ " "+nonce + " "+ signature+ " " + localSign);
    	
    	return localSign.equals(signature) ? true:false;
    }
    
    /**
     * @param connection current connection
     * @param ctx
     */
    private void messageProcess(Connection currConnection){
    	
    	String message = currConnection.getMsg();
    	JSONObject para = JSON.parseObject(message);

    	ChannelHandlerContext ctx = currConnection.getChannelHandlerContext();
    	
    	int type = para.getInteger(Message.MessageType);
    	
    	currConnection.parseAndSetClientPara();
    	
    	switch (type) {
		case 1:
			//外部请求, 放入队列
			server.allConnChannelQueue.add(currConnection);
			logger.info("新增任务，任务数:"+ server.allConnChannelQueue.size());

			wakeupWaitingWorkIfNeed();
			
//			currConnection.setEndTime(System.currentTimeMillis() + 5000);
			timeoutQueue.add(currConnection);

			break;
		case 2:
			//worker请求
			processWorkerRequest(currConnection);
			break;
		case 3:
			//worker完成任务，返回数据
			logger.info("完成任务");
	        String uuid_connId = para.getString(Message.ConnId);
	        int status = para.getInteger(Message.Status);
	        
	        JSONObject result = new JSONObject();
	        if(status == 0){
	        	float score = para.getFloat(Message.Score);
	        	result.put(Message.Status, 0);
	        	result.put(Message.Score, score);
	        	result.put(Message.ConnId, decodeConnId(uuid_connId));
	        }
	        else {
	        	result.putAll(para);
	        	result.put(Message.MessageType, 1);
	        	System.out.println("result error." + para.toJSONString());
			}
//	        logger.info("Result:" + SystemUtil.addNewLine(result.toJSONString()));
	        byte[] re = SystemUtil.addNewLine(result.toJSONString()).getBytes();
	        ByteBuf bb3 = Unpooled.buffer(1024);
	        bb3.writeBytes(re);
	        Connection conn = server.consumingChannel.get(String.valueOf(uuid_connId));
			logger.info("consumingChannel size:" + server.consumingChannel.size());

			if (conn == null) {
				logger.error("not found connid in workqueue:" + uuid_connId);
				return;
			}
	        
	        //todo判断任务是否超时
	        conn.getChannelHandlerContext().writeAndFlush(bb3);
	        server.consumingChannel.remove(uuid_connId);
			break;
		default:
			System.out.println("type error!");
			break;
		}
    }

    public void wakeupWaitingWorkIfNeed(){
    	Connection waitWorker = server.waitingQueue.poll();
    	
    	if (waitWorker != null) {
    		logger.info("wakeup worker.");
    		processWorkerRequest(waitWorker);
		}
    }
    
	public void processWorkerRequest(Connection workerConn) {

		Connection task = null;
		while(true){
			task = server.allConnChannelQueue.poll();
			if (task == null) {
				break;
			}
			else if (task.getIsTimeOut()) {
				logger.info("processWorkerRequest: connection time out." + task.getMsg());
				/**
				 * todos 超时后是否需要通知client
				 */
			}
			else {
				break;
			}
		}

		JSONObject resultJson = new JSONObject();

		if (task != null) {
			logger.info("worker消费任务, 任务数:" + server.allConnChannelQueue.size());
			JSONObject taskJson = JSON.parseObject(task.getMsg());
			String taskData = taskJson.getString(Message.DATA);
			String connId = taskJson.containsKey(Message.ConnId) ? taskJson
					.getString(Message.ConnId) : String.valueOf(Math.abs(task
					.hashCode()));

			// 后期需要增加容错机制，consumingChannel为正在处理中的任务，需要对任务长期执行出错的处理
			String uuid_conn_id = encodeConnId(connId);
			
			server.consumingChannel.put(uuid_conn_id, task);
			resultJson.put(Message.MessageType, 2);
			resultJson.put(Message.DATA, taskData);
			resultJson.put(Message.ConnId, uuid_conn_id);
			resultJson.put(Message.Status, 0);
		} else {
			server.waitingQueue.add(workerConn);
			resultJson.put(Message.Status, -1);
			resultJson.put(Message.StatusMessage, "no task now");
			return;
		}

		ChannelHandlerContext ctx = workerConn.getChannelHandlerContext();

		byte[] data = SystemUtil.addNewLine(resultJson.toJSONString())
				.getBytes();
		ByteBuf bb = Unpooled.buffer(1024);
		bb.writeBytes(data);
		ctx.writeAndFlush(bb);
	}
	
	private String encodeConnId(String connid) {
		UUID randomUuid = UUID.randomUUID();
		return randomUuid.toString().replace("-", "").concat(connid);
	}
	
	private String decodeConnId(String id) {
		if (id.length() > SystemConstants.UUIDLength) {
			return id.substring(SystemConstants.UUIDLength);
		}
		return "0";
	}

	private void returnMessageToClient(String resultMessage, ChannelHandlerContext chx) {
		resultMessage += "\n";
		ByteBuf byteBuf = Unpooled.buffer(1024);
		byteBuf.writeBytes(resultMessage.getBytes());
		chx.writeAndFlush(byteBuf);
	}

	private void returnStandardMessageToClient(int status, String messg, String data, ChannelHandlerContext chx){
    	
    	JSONObject jo = new JSONObject();
    	jo.put(Message.Status, status);
    	jo.put(Message.StatusMessage, messg);
    	jo.put(Message.DATA, data);

		String result = SystemUtil.addNewLine(jo.toJSONString());
    	
    	ByteBuf byteBuf = Unpooled.buffer(1024);
    	byteBuf.writeBytes(result.getBytes());
    	chx.writeAndFlush(byteBuf);
    }
    
    public static void main(String[] args) {
		System.out.println(SHA1.sha1("huchengjian".getBytes()));
	}
    

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        logger.info("server channelReadComplete..");
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("server exceptionCaught..");
		logger.error(cause.getMessage());
		cause.printStackTrace();
		returnStandardMessageToClient(Status.ServerError,
				SystemUtil.addNewLine(Status.ServerErrorMessg), null, ctx);
//        ctx.close();
    }
}