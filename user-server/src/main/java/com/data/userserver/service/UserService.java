package com.data.userserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.data.userserver.entity.RegisterVo;
import com.data.userserver.entity.User;

/**
 * @author User
 * @date 2022/7/14 17:19
 */
public interface UserService extends IService<User> {
    void register(RegisterVo registerVo);

    User getUserByName(String username);

//    String login(RegisterVo registerVo) ;
    void updateUserAvatar(String urlPermanent);
}
