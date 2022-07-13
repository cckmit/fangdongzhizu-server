package com.data.signal.impl;

import com.data.signal.abstracts.BaseServer;
import com.data.signal.handler.GatewayHandler;
import com.data.signal.handler.ImageHandler;
import com.data.signal.handler.MessageHandler;
import com.data.signal.handler.VoiceHandler;
import com.data.signal.utils.NettyUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatServer extends BaseServer {


    // 服务端接受数据接口
    private static final Integer PORT = 17180;

    private ScheduledExecutorService executorService;

    public ChatServer() {
        executorService = Executors.newScheduledThreadPool(2);
    }

    /**
     * 启动资源
     */
    @Override
    public void start() {
        sb.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .localAddress(new InetSocketAddress(PORT))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(defaultLoopGroup,
                                new HttpServerCodec(),   //请求解码器
                                new HttpObjectAggregator(65536), //将多个消息转换成单一的消息对象
                                new ChunkedWriteHandler(),  //支持异步发送大的码流，一般用于发送文件流
                                new IdleStateHandler(600, 0, 0), //检测链路是否读空闲
                                new GatewayHandler(),   //处理握手和认证
                                new MessageHandler(),   //处理消息的发送
                                new ImageHandler(),     //处理图片的发送
                                new VoiceHandler()      //处理语音的发送
                        );
                    }
                });
        try {
            cf = sb.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            log.info("-------通信服务已成功启动, 端口是:{}-------", addr.getPort());

            // 定时扫描所有的Channel，关闭失效的Channel
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    log.info("--------定时扫描所有的Channel，关闭失效的Channel--------");
                    NettyUtil.scanNotActiveChannel();
                }
            }, 3, 60, TimeUnit.SECONDS);

            // 定时向所有客户端发送ping消息
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    log.info("--------定时向所有客户端发送ping消息--------");
                    NettyUtil.broadCastPingMsg();
                }
            }, 3, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("-------通信服务启动失败-------", e);
        }
    }

    /**
     * 关系资源
     */
    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.shutdown();
    }

}
