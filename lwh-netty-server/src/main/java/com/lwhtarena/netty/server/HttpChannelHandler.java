package com.lwhtarena.netty.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p></P>
 */
public class HttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest>{

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        /**检测解码情况**/
        if(!request.getDecoderResult().isSuccess()){
            sendError(ctx, BAD_REQUEST);
            return;
        }

        final String uri =request.getUri();
        final String path = sanitizeUri(uri);

        if(path ==null){
            sendError(ctx, FORBIDDEN);
            return;
        }

        /**读取要下载的文件**/
        File file = new File(path);
        if(file.isHidden()||!file.exists()){
            sendError(ctx, NOT_FOUND);
            return;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        RandomAccessFile raf;

        try {
            raf = new RandomAccessFile(file,"r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        long fileLength = raf.length();
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpHeaders.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);

        if (!HttpHeaders.isKeepAlive(request)) {
        } else {
            response.headers().set("CONNECTION", HttpHeaders.Values.KEEP_ALIVE);
        }

        ctx.write(response);

        ChannelPromise channelPromise =ctx.newProgressivePromise();

        /**
         * ChunkedFile 从一个文件一块一块的获取数据的ChunkedInput.
         * 【】ChunkedFile(java.io.RandomAccessFile file, long offset, long length, int chunkSize)
         *     创建一个从指定文件获取数据的实例.
         *
         *  HttpChunkedInput(ChunkedInput<ByteBuf> input,LastHttpContent lastHttpContent)
         *
         *  chunkField 8M -->8192kb
         */
        ChannelFuture sendFileFuture =
                ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), channelPromise);

        /**sendFuture 用于监视发送数据的状态**/
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + "Transfer progress:" + progress);
                    setDataToClient(response,String.valueOf(progress));
                    ctx.write(response);
                } else {
                    System.err.println(future.channel() + "Transfer progress:" + progress / total);
                    setDataToClient(response,String.valueOf(progress));
                    ctx.write(response);
                }
            }



            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + "Transfer complete.");
                setDataToClient(response,"Transfer complete.");
                ctx.write(response);
            }
        });

//写结束标记
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

//决定是否关闭连接。
        if (!HttpHeaders.isKeepAlive(request)) {
//当整个内容被写出时关闭连接。
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
        ctx.close();
    }

    /**
     * 出现解码异常,并关闭
     * @param ctx
     * @param status
     * @description:<p>
     *  发送响应信息回去
     * </p>
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: "+ status +"\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE,"text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        uri = uri.replace('/', File.separatorChar);

        if (uri.contains(File.separator +'.') || uri.contains('.'+ File.separator) || uri.startsWith(".") || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        return File.separator + uri;
    }

    /**
     * Sets the content type header for the HTTP Response
     * @param response
     * @param file
     */
    private void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap m = new MimetypesFileTypeMap();
        String contentType = m.getContentType(file.getPath());
        if (!contentType.equals("application/octet-stream")) {
            contentType += "; charset=utf-8";
        }
        response.headers().set(CONTENT_TYPE, contentType);
    }

    private void setDataToClient(HttpResponse response, String progress) {
        response.headers().set(CONTENT_TYPE,"text/plain; charset=UTF-8");
        response.headers().set("progress",progress);
    }

}
