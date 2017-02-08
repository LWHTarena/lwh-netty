package com.lwhtarena.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;


/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p></P>
 */
public class HttpChannelInitlalizer extends ChannelInitializer<SocketChannel>{

    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline =ch.pipeline();

        pipeline.addLast(new HttpServerCodec());

        pipeline.addLast(new HttpObjectAggregator(65536));

        pipeline.addLast(new ChunkedWriteHandler());

        /**自定义业务逻辑handler**/
        pipeline.addLast(new HttpChannelHandler());

    }

    /**
     * 最后来看下http的几个ChannelHandler
     *  服务端：ChunkedWriteHandler
     *  客户端：HttpObjectAggregator
     *
     * 一般http请求或者响应,解码器都将其解码成为多个消息对象,主要是httpRequest/httpResponse, httpcontent,
     * lastHttpContent.然后反复调用messageReceive这个方法,HttpObjectAggregator 这个handler就是将同一个
     * http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象ChunkedWriteHandler 这个handler
     * 主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的,增加ChunkedWriteHandler 这
     * 个handler我们就不用考虑这个问题了,内部原理看源代码.
     */
}
