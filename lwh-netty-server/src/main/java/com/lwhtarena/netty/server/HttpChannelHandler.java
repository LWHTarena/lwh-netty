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
 * @Date: 2017/2/9.
 * @Description：<p></P>
 */
public class HttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    int num =0;
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        System.out.println("=========---"+num+"----========");
        num++;
// 监测解码情况i
        if (!request.getDecoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        final String uri = request.getUri();
        final String path = sanitizeUri(uri);
        System.out.println("get file:"+path);
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }
// 读取要下载的文件
//        File file = new File("D:/lwhSpaces/windows2008.xva");
        File file = new File(path);
        System.out.println("读取文件大小："+file.length());
        if (file.isHidden() || !file.exists()) {
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
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpHeaders.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);

//setDateAndCacheHeaders(response, file);
        if (!HttpHeaders.isKeepAlive(request)) {
        } else {
            response.headers().set("CONNECTION", HttpHeaders.Values.KEEP_ALIVE);
        }

// Write the initial line and the header.
        ctx.write(response);

        System.out.println("文件大小："+fileLength);


        ChannelPromise channelPromise =ctx.newProgressivePromise();
// Write the content.
        ChannelFuture sendFileFuture =

                /**
                 * ChunkedFile 从一个文件一块一块的获取数据的ChunkedInput.
                 * 【】ChunkedFile(java.io.RandomAccessFile file, long offset, long length, int chunkSize)
                 *     创建一个从指定文件获取数据的实例.
                 *
                 *  HttpChunkedInput(ChunkedInput<ByteBuf> input, LastHttpContent lastHttpContent)
                 *
                 *  chunkField 8M -->8192kb
                 */

                ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), channelPromise);

// sendFuture 用于监视发送数据的状态
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + "Transfer progress:" + progress);
                } else {
                    System.err.println(future.channel() + "Transfer progress:" + progress / total);
                }
            }

            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + "Transfer complete.");
            }
        });


// Write the end marker
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

// Decide whether to close the connection or not.
        if (!HttpHeaders.isKeepAlive(request)) {
// Close the connection when the whole content is written out.
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

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private static String sanitizeUri(String uri) {
// Decode the path.
        try {
            uri = URLDecoder.decode(uri,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

//        if (!uri.startsWith("/")) {
//            return null;
//        }

// Convert file separators.
        uri = uri.replace('/', File.separatorChar);

// Simplistic dumb security check.
// You will have to do something serious in the production environment.
        if (uri.contains(File.separator +'.') || uri.contains('.'+ File.separator) || uri.startsWith(".") || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

// Convert to absolute path.
//        return SystemPropertyUtil.get("user.dir") + File.separator + uri;
        return File.separator + uri;
    }


    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: "+ status +"\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE,"text/plain; charset=UTF-8");

// Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response
     * HTTP response
     * @param file
     * file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap m = new MimetypesFileTypeMap();
        String contentType = m.getContentType(file.getPath());
        if (!contentType.equals("application/octet-stream")) {
            contentType += "; charset=utf-8";
        }
        response.headers().set(CONTENT_TYPE, contentType);
    }

    private static void setDataToClient(HttpResponse response,String progress){
        response.headers().set("progress",progress);
    }
}

