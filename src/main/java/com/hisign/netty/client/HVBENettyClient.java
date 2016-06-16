package com.hisign.netty.client;

import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisign.bean.Message;
import com.hisign.test.ClentDemoChannelHandler;

/**
 * Created by hugo on 6/14/16.
 */
public class HVBENettyClient {

    private String host;
    private String port;
    
    private JSONObject requestJson;
    private String resultStr;
    
    public HVBENettyClient(String host, String port){
    	this.host = host;
    	this.port = port;
    	requestJson = new JSONObject();
    }
    
    public void setRequestParameter(Map<String, Object> para){
    	String paraStr = JSON.toJSONString(para); 
    	
    	requestJson.put(Message.MessageType, 1);
    	requestJson.put(Message.DATA, paraStr);
    }
    
    public static void main(String[] args) {
    	HVBENettyClient sClient = new HVBENettyClient("", "");
    	Map<String, Object> para = new HashMap<String, Object>();
    	para.put("a", "012302103021030210312");
    	para.put("aa", "fdsaf");
    	para.put("aaa", 12);
    	sClient.setRequestParameter(para);
    	System.out.println(sClient.requestJson.toJSONString());
	}

    public JSONObject compareTwoFace(){

        JSONObject jo = new JSONObject();

        return jo;
    }
    
    public void connect(String host, int port) throws Exception {

        //创建事件循环组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            //创建引导程序
            Bootstrap b = new Bootstrap();
            //设置消息循环
            b.group(group);
            //设置通道
            b.channel(NioSocketChannel.class);
            //配置通道参数：tcp不延迟
            b.option(ChannelOption.TCP_NODELAY, true);
            //设置通道处理
            b.handler(new ClentDemoChannelHandler());
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new HVBENettyClientHandler(requestJson.toJSONString(), resultStr));
                }
            });

            //发起异步链接，等待输入参数
            
            ChannelFuture f = b.connect(host, port).sync();
            // 等待客户端链路关闭
            f.channel().closeFuture().sync();
        } finally {
            //关闭
            group.shutdownGracefully();
        }

    }



}
