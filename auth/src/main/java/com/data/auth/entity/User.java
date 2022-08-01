package com.data.auth.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author User
 * @date 2022/7/13 21:29
 */
@Data
public class User implements Serializable {
    //手机号即用户名
    private String username;

    private String password;
    // 验证码
    /*private String code;
    private String rememberMe;*/
}
