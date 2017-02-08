package com.lwhtarena.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p></P>
 */
public class HttpDownloadClient {

    /**
     * 下载 http 资源 向服务器下载直接填写要下载的文件的相对路径
     * @param host 目的主机 ip 或域名
     * @param port 目标主机端口
     * @param url 文件路径
     * @param local 本地存储路径 ===文件名
     * @throws Exception
     */
    public void connect(String host, int port, String url, final String local) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bs =new Bootstrap();
            bs.group(workerGroup);
            bs.channel(NioSocketChannel.class);
            bs.option(ChannelOption.SO_KEEPALIVE,true);
            bs.handler(new ChildChannelHandler(local));

            //启动 ---> 客户端
            ChannelFuture chf =bs.connect(host,port).sync();

            URI uri = new URI(url);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());

            //构建 http 请求
            request.headers().set(HttpHeaders.Names.HOST,host);
            request.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());

            //发送 http 请求
            chf.channel().write(request);
            chf.channel().flush();
            chf.channel().closeFuture().sync();

        }finally {
            /*优雅关闭*/
            workerGroup.shutdownGracefully();
        }

    }
}
