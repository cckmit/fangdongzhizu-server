package com.data.userserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.data.userserver.mapper")
@ComponentScan(basePackages = "com.data")
//开启通过注解进行权限角色验证功能  @secured @preAuthorize
//@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)
public class UpmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpmApplication.class, args);
    }

}
