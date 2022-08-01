package com.data.auth.filter;

import com.alibaba.fastjson.JSON;
import com.data.common.Response;
import com.data.common.utils.ResponseUtil;
import com.data.auth.entity.SecurityUser;
import com.data.auth.entity.User;
import com.data.auth.security.TokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author User
 * @date 2022/7/13 21:37
 */
public class TokenLoginFilter extends UsernamePasswordAuthenticationFilter {
    private AuthenticationManager authenticationManager;
    private TokenManager tokenManager;
    private StringRedisTemplate redisTemplate;

    public TokenLoginFilter(AuthenticationManager authenticationManager, TokenManager tokenManager, StringRedisTemplate redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.tokenManager = tokenManager;
        this.redisTemplate = redisTemplate;
        this.setPostOnly(false);
        //this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/users/login","POST"));
        this.setFilterProcessesUrl("/users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException {
        try {
           // 登录如果包含别的参数，默认传空字符串即可
            User user = new ObjectMapper().readValue(req.getInputStream(), User.class);
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), new ArrayList<>()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 登录成功
     * @param req
     * @param res
     * @param chain
     * @param auth
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        String token = tokenManager.createToken(user.getUserInfo().getUsername());
        redisTemplate.opsForValue().set(user.getUserInfo().getUsername(), JSON.toJSONString(user.getPermissionList()).toString());
        logger.info("successfulAuthentication=="+token);
        ResponseUtil.out(res, Response.ok().data("token", token));
    }

    /**
     * 登录失败
     * @param request
     * @param response
     * @param e
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException e) throws IOException, ServletException {
        if (e instanceof AccountExpiredException) {
            //账号过期
            ResponseUtil.out(response, Response.error().message("账户过期"));
        } else if (e instanceof BadCredentialsException) {
            //密码错误
            ResponseUtil.out(response, Response.error().message("密码错误"));
        } else if (e instanceof CredentialsExpiredException) {
            //密码过期
            ResponseUtil.out(response, Response.error().message("密码过期"));
        } else if (e instanceof DisabledException) {
            //账号不可用
            ResponseUtil.out(response, Response.error().message("账号不可用"));
        } else if (e instanceof LockedException) {
            //账号锁定
            ResponseUtil.out(response, Response.error().message("账号锁定"));
        } else if (e instanceof InternalAuthenticationServiceException) {
            //用户不存在
            ResponseUtil.out(response, Response.error().message("用户不存在"));
        }else{
            //其他错误
            ResponseUtil.out(response, Response.error());
        }

    }
}
