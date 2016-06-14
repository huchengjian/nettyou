package com.hisign.netty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class NettyServerHandler extends ChannelHandlerAdapter {
	
	private NettyServer server;
	
    static private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);
	
	NettyServerHandler(NettyServer server){
		this.server = server;
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
    	
        System.out.println("server channelRead..");
        
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("Receive Message: " + body);
        JSONObject jo = JSONObject.parseObject(body);
        
        logger.info(jo.getString("DATA"));
        Connection connection = new Connection(ctx, jo.getString(Message.DATA));
        messageProcess(body, connection, ctx);
        
        
//        System.out.println("The time server receive order:" + body);
//        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(
//                System.currentTimeMillis()).toString() : "BAD ORDER";
//        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
//        ctx.write(resp);
    }
    
    /**
     * 
     * @param message 
     * @param connection current connection
     * @param ctx
     */
    private void messageProcess(String message, Connection connection, ChannelHandlerContext ctx){
    	
    	logger.info("messageProcess");
    	
    	JSONObject js = JSON.parseObject(message);
    	int type = (int) js.get(Message.MessageType);
    	
    	switch (type) {
		case 1:
			//外部请求, 放入队列,后期需要增加容错机制.
			server.allConnChannelQueue.add(connection);
			logger.info("新增任务，任务数:"+ server.allConnChannelQueue.size());
			break;
		case 2:
			//worker 请求
			Connection task = server.allConnChannelQueue.poll();
			logger.info("worker消费任务, 任务数:"+ server.allConnChannelQueue.size());
			JSONObject jo = new JSONObject();
			if(task != null){
				server.consumingChannel.put(String.valueOf(task.hashCode()), task);
				jo.put(Message.MessageType, 2);
				jo.put(Message.DATA, task.getMsg());
				jo.put(Message.ConnId, task.hashCode());
				jo.put(Message.Status, 0);
			}
			else{
				jo.put(Message.Status, -1);
			}
        	
        	byte[] data = jo.toJSONString().getBytes();
        	ByteBuf bb = Unpooled.buffer(1024);
        	bb.writeBytes(data);
        	ctx.writeAndFlush(bb);
			break;
		case 3:
			//worker完成任务，返回数据
			logger.info("完成任务");
			JSONObject para = JSON.parseObject(message);
//	        String conn = (String) para.get("Data");
	        int connId = (int) para.get(Message.ConnId);
	        
	        Connection conn = server.consumingChannel.get(String.valueOf(connId));
	        
	        byte[] re = para.toJSONString().getBytes();
	        ByteBuf bb3 = Unpooled.buffer(1024);
	        bb3.writeBytes(re);
	        conn.getChannelHandlerContext().writeAndFlush(bb3);
			ctx.close();
			break;
		default:
			System.out.println("type error!");
			
			break;
		}
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("server channelReadComplete..");
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println("server exceptionCaught..");
        System.out.println(cause.getMessage());
        ctx.close();
    }
}