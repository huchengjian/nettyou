package com.netty.start.test;

import com.netty.start.server.NettyServer;

/**
 * Created by chenhao on 2016/3/17.
 */
public class ServerTest {

    public static void main(String[] args) throws Exception {

        NettyServer server = new NettyServer();
        server.start(3000);

    }

}
