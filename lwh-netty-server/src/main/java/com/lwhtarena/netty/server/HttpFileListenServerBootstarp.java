package com.lwhtarena.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p>服务端</P>
 */
public class HttpFileListenServerBootstarp implements Runnable{

    private  int port;

    public HttpFileListenServerBootstarp(int port) {
        this.port = port;
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);

        serverBootstrap.childHandler(new HttpChannelInitlalizer());
        try {
            ChannelFuture chf = serverBootstrap.bind(port).sync();
            chf.channel().closeFuture().sync();
        }catch (InterruptedException e){
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) {
        HttpFileListenServerBootstarp serve =new HttpFileListenServerBootstarp(9003);
        serve.run();
    }
}
