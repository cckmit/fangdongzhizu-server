package com.data.auth.config;


import com.data.auth.filter.TokenAuthenticationFilter;
import com.data.auth.filter.TokenLoginFilter;
import com.data.auth.security.DefaultPasswordEncoder;
import com.data.auth.security.TokenLogoutHandler;
import com.data.auth.security.TokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * @author User
 * @date 2022/7/1 21:23
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)
public class TokenWebSecurityConfig extends WebSecurityConfigurerAdapter {
    private UserDetailsService userDetailsService; //自己写的查询数据库类
    private TokenManager tokenManager;//token生成工具类
    private DefaultPasswordEncoder defaultPasswordEncoder;//密码处理
    private StringRedisTemplate stringRedisTemplate;//操作redis

    @Autowired
    public DataSource dataSource;
    /*@Autowired
    PersistentTokenRepository persistentTokenRepository;*/

    @Bean
    public PersistentTokenRepository persistentTokenRepository(){
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        //jdbcTokenRepository.setCreateTableOnStartup(true);
        return jdbcTokenRepository;
    }

    @Autowired
    public TokenWebSecurityConfig(UserDetailsService userDetailsService, DefaultPasswordEncoder defaultPasswordEncoder,
                                  TokenManager tokenManager, StringRedisTemplate stringRedisTemplate) {
        this.userDetailsService = userDetailsService;
        this.defaultPasswordEncoder = defaultPasswordEncoder;
        this.tokenManager = tokenManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(defaultPasswordEncoder);
    }

    /*在此方法中指定的端点将被Spring Security忽略
    不会保护他们免受CSRF, XSS，点击劫持*/
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/api/**",
                "/swagger-resources/**", "/webjars/**", "/v2/**", "/swagger-ui.html/**"
        );//设置哪些路径不做拦截，如swagger等
//        web.ignoring().antMatchers("/*/**"
//        );
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
                .exceptionHandling()
//                .authenticationEntryPoint(new UnauthorizedEntryPoint())
                .and().csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .antMatchers("/users/register").permitAll()//都可以访问
                .anyRequest().authenticated()//登录后可访问
                .and().logout()
                .logoutUrl("/users/logout")//设置退出地址
                .addLogoutHandler(new TokenLogoutHandler(tokenManager,stringRedisTemplate))
                //.and().rememberMe().rememberMeParameter("rememberMe").tokenRepository(persistentTokenRepository()).tokenValiditySeconds(120).userDetailsService(userDetailsService)
                .and()
                .addFilter(new TokenLoginFilter(authenticationManager(), tokenManager, stringRedisTemplate))
                .addFilter(new TokenAuthenticationFilter(authenticationManager(), tokenManager, stringRedisTemplate)).httpBasic();
    }
}
