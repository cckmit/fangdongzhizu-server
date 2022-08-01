package com.data.userserver.service.impl;

import com.data.auth.entity.SecurityUser;
import com.data.userserver.entity.User;
import com.data.userserver.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author User
 * @date 2022/7/15 11:38
 */
@Service("userDetailsService")
public class CoustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username)  {
        User user = userService.getUserByName(username);

        com.data.auth.entity.User  user1 = new com.data.auth.entity.User();
        BeanUtils.copyProperties(user,user1);
        // todo 从数据库 获取权限列表，暂时写死
        List<String> authorities = new ArrayList<>();

        authorities.add("permission1");
        authorities.add("admin");
        SecurityUser securityUser = new SecurityUser(user1);
        securityUser.setPermissionList(authorities);
        return securityUser;

    }
}
