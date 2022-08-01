package com.data.userserver;

import com.data.signal.impl.ChatServer;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@MapperScan("com.data.userserver.mapper")
@ComponentScan(basePackages = "com.data")
//开启通过注解进行权限角色验证功能  @secured @preAuthorize
//@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)
public class UpmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpmApplication.class, args);

        // 启用聊天模块
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
