package com.data.userserver.entity;

import lombok.Data;

/**
 * @author User
 * @date 2022/7/14 18:02
 */
@Data
public class RegisterVo {
    //手机号即用户名
    private String username;
    private String password;
    // 验证码
    private String code;
    private String rememberMe;
}
