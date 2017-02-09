package com.lwhtarena.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;

/**
 * @author： liwh
 * @Date: 2017/2/9.
 * @Description：<p></P>
 */
public class HttpDownloadClient {
    /**
     * 下载 http 资源 向服务器下载直接填写要下载的文件的相对路径
     * （↑↑↑建议只使用字母和数字对特殊字符对字符进行部分过滤可能导致异常↑↑↑）
     * 向互联网下载输入完整路径
     * @param host 目的主机 ip 或域名
     * @param port 目标主机端口
     * @param url 文件路径
     * @param local	本地存储路径 ===文件名
     * @throws Exception
     */
    public void connect(String host, int port, String url, final String local) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChildChannelHandler(local));


///===========>>>>> Start the client.
            ChannelFuture f = b.connect(host, port).sync();

            URI uri = new URI(url);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());

///===========>>>>> 构建 http 请求
            request.headers().set(HttpHeaders.Names.HOST, host);
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());

///===========>>>>> 发送 http 请求
            f.channel().write(request);
            f.channel().flush();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    public static void main(String[] args) throws Exception {
        HttpDownloadClient client = new HttpDownloadClient();
        client.connect("127.0.0.1", 9003,"D:/lwhSpaces/TemplateRepo/Centos7.1bit64.xva","temp_upload.xva");



//        client.connect("192.168.222.44", 9003,"/tmp/Centos7.1bit64.xva","temp_upload.xva");
//client.connect("zlysix.gree.com", 80, "http://zlysix.gree.com/HelloWeb/download/20m.apk", "20m.apk");

    }
}
