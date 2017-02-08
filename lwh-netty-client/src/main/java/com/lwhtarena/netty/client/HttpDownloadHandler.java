package com.lwhtarena.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author： liwh
 * @Date: 2017/2/8.
 * @Description：<p></P>
 */
public class HttpDownloadHandler extends ChannelInboundHandlerAdapter{
    // 分块读取开关
    private boolean readingChunks = false;
    // 文件输出流
    private FileOutputStream fOutputStream = null;
    // 下载文件的本地对象
    private File localfile = null;
    // 待下载文件名
    private String local = null;
    // 状态码
    private int succCode;
    //进度
    private long process;

    public HttpDownloadHandler(String local) {
        this.local = local;
    }

    /**
     * @param ctx
     * @param msg
     * @throws Exception
     * @description:<p>
     *  接收响应信息
     * </p>
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if(msg instanceof HttpResponse){
            HttpResponse response = (HttpResponse) msg;
            /**获取状态码**/
            succCode =response.getStatus().code();
            if(succCode == 200){
                /**设置下载文件存放地址**/
                setDownLoadFile();
                /**是否分块传输**/
                readingChunks = true;
            }
            if(succCode==404){
                System.out.println("请求文件不存在");
            }
        }


        if(msg instanceof HttpContent){
            System.out.println("文件的长度："+new File(localfile.getAbsolutePath()).length());

            HttpContent chunk = (HttpContent) msg;

            /**是否是最后一段传输**/
            if (chunk instanceof LastHttpContent) {
                readingChunks = false;
            }

            ByteBuf buffer = chunk.content();
            byte[] dst = new byte[buffer.readableBytes()];

            if(succCode ==200 ){
                while (buffer.isReadable()){
                    buffer.readBytes(dst);
                    fOutputStream.write(dst);
                    buffer.release();
                }
                if(null !=fOutputStream){
                    fOutputStream.flush();
                }
            }
        }


        if(!readingChunks){
            if(null!=fOutputStream){
                System.out.println("下载完成！文件的路径："+localfile.getAbsolutePath());
                fOutputStream.flush();
                fOutputStream.close();
                localfile =null;
                fOutputStream =null;
            }

            /**关闭ChannelHandlerContext**/
            ctx.channel().close();
        }

    }


    /**
     * 捕获异常
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("文件传输异常");
    }

    /**
     * 传输块完成
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("有一段字节数组传输完成！");
    }

    /**
     * 配置本地参数，准备下载
     * 获取应用系统路径
     */
    private void setDownLoadFile() throws Exception{
        if (null == fOutputStream) {
            local = SystemPropertyUtil.get("user.dir") + File.separator +local;
            System.out.println("================>>>>>>>>>>>>>>>>>>>>>");
            System.out.println(local);
            localfile = new File(local);
            if (!localfile.exists()) {
                localfile.createNewFile();
            }
            fOutputStream = new FileOutputStream(localfile);
        }
    }


}
