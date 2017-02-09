package com.lwhtarena.netty.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author： liwh
 * @Date: 2017/2/9.
 * @Description：<p></P>
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
    String local;
    public ChildChannelHandler(String local) {
        this.local = local;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

///===========>>>>>客户端接收到的是 httpResponse 响应，所以要使用 HttpResponseDecoder 进行解码
        ch.pipeline().addLast(new HttpResponseDecoder());

///===========>>>>>客户端发送的是 httprequest，所以要使用 HttpRequestEncoder 进行编码
        ch.pipeline().addLast(new HttpRequestEncoder());
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new HttpDownloadHandler(local));
    }

}
