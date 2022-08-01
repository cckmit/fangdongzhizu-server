package com.data.userserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.data.auth.security.TokenManager;
import com.data.common.exception.UserException;
import com.data.userserver.entity.RegisterVo;
import com.data.userserver.entity.User;
import com.data.userserver.mapper.UserMapper;
import com.data.userserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author User
 * @date 2022/7/14 18:40
 */
@Component
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    public BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public TokenManager tokenManager;

    @Override
    public void register(RegisterVo registerVo) {
        String code = registerVo.getCode();
        String password = registerVo.getPassword();
        String username = registerVo.getUsername();
        if (StringUtils.isEmpty(code) ||  StringUtils.isEmpty(password) || StringUtils.isEmpty(username)  ){
            throw new UserException(20002,"参数有空，注册失败");
        }
        // TODO 验证code码

        //  用户名是否重复
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0){
            throw new UserException(20002,"用户名已被使用，注册失败");
        }
        User user = new User();
        user.setUsername(username);
        // 昵称默认等于 username
        user.setNickname(username);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setIsDisabled(false);
        //头像给默认值
        user.setAvatar("http://81.70.163.240:25415/avatar-bk/62e20350efcef62d940556e420220726132059.jpg");
        baseMapper.insert(user);
    }

    @Override
    public User getUserByName(String username) {

        return baseMapper.selectOne(new QueryWrapper<User>().eq("username",username));
    }

 /*   @Override
    public String login(RegisterVo registerVo) {
        String username = registerVo.getUsername();
        String password = registerVo.getPassword();

        // todo 验证码功能
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            throw new UserException(20002,"用户名或密码为空");
        }
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username);
        User user = baseMapper.selectOne(wrapper);
        if (user == null){
            throw new UserException(20002,"用户名或密码错误！");
        }
        if(!bCryptPasswordEncoder.matches(password,user.getPassword())){
            throw new UserException(20002,"用户名或密码错误！");
        }
        if(user.getIsDisabled()){
            throw new UserException(20002,"用户被禁用！");
        }
        //生成jwtToken
        String token = tokenManager.createToken(user.getUsername());
//        String token = JwtUtils.getJwtToken(user.getId(), user.getUsername());
        System.out.println("token::"+token);
        return token;
    }*/

    @Override
    public void updateUserAvatar(String urlPermanent) {
        if(StringUtils.isEmpty(urlPermanent)){
            throw new UserException(20002,"参数有误！");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        User user = new User();
        user.setUsername(currentPrincipalName);
        user.setAvatar(urlPermanent);
        /*user.setUpdateTime(new Date());*/
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("username",currentPrincipalName);
        baseMapper.update(user, userQueryWrapper);
    }
}
