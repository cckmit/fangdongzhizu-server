package com.data.signal;

import com.data.signal.impl.ChatServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class SignalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalApplication.class, args);
        final ChatServer server = new ChatServer();
        server.init();
        server.start();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                server.shutdown();
                log.warn("----------服务器清理资源完毕，服务正在关闭中ing----------");
                System.exit(0);
            }
        });
    }

}
