package com.hzgc.service.dyncar.config;

import com.hzgc.common.service.rest.BigDataPath;
import org.springframework.boot.autoconfigure.security.Http401AuthenticationEntryPoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

/**
 * @author liuzhikun
 * @date 2018/05/17
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 过滤登录，登出，获取菜单接口的权限
        http.
                csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(new Http401AuthenticationEntryPoint("Bearer realm=\"webrealm\""))
                .and()
                .authorizeRequests()
                .antMatchers(BigDataPath.DYNREPO_GETPICTURE + "/**").permitAll()
                .antMatchers("**").authenticated()
                .and()
                .httpBasic();
    }

}
