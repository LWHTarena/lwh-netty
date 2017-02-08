package com.lwhtarena.netty.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;


/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p></P>
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    String local;

    public ChildChannelHandler(String local) {
        this.local = local;
    }

    protected void initChannel(SocketChannel sc) throws Exception {

        /**--客户端接收到的是 httpResponse 响应，所以要使用 HttpResponseDecoder 进行解码--**/
        sc.pipeline().addLast(new HttpResponseDecoder());

        /**--客户端发送的是 httprequest，所以要使用 HttpRequestEncoder 进行编码--**/
        sc.pipeline().addLast(new HttpRequestDecoder());

        /**--大文件传输，防止内训泄露--**/
        sc.pipeline().addLast(new ChunkedWriteHandler());

        /**--自定义业务逻辑handler--**/
        sc.pipeline().addLast(new HttpDownloadHandler(local));
    }

}
