package com.data.signal.abstracts;

import com.data.signal.interfaces.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseServer implements Server {


    /**
     * NioEventLoopGroup是用来处理I/O操作的多线程事件循环器
     * 在这个例子中我们实现了一个服务端的应用，因此会有2个 NioEventLoopGroup 会被使用。
     * 第一个经常被叫做’boss’，用来接收进来的连接。第二个经常被叫做‘worker’，用来处理已经被接收的连接，一旦‘boss’接收到连接，就会把连接信息注册到‘worker’上
     */
    protected DefaultEventLoopGroup defaultLoopGroup;
    protected NioEventLoopGroup bossGroup;
    protected NioEventLoopGroup workGroup;
    protected NioServerSocketChannel ssc;
    protected ChannelFuture cf;
    protected ServerBootstrap sb;


    /**
     * ServerBootstrap是一个启动 NIO 服务的辅助启动类
     */
    public void init(){
        defaultLoopGroup = new DefaultEventLoopGroup(8, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "默认事件组-" + index.incrementAndGet());
            }
        });
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "主线程-" + index.incrementAndGet());
            }
        });
        workGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 10, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "辅助线程-" + index.incrementAndGet());
            }
        });

        sb = new ServerBootstrap();
    }

    @Override
    public void shutdown() {
        if (defaultLoopGroup != null) {
            defaultLoopGroup.shutdownGracefully();
        }
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

}
