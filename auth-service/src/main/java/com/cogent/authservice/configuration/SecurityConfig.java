package com.cogent.authservice.configuration;

import static com.cogent.authservice.constants.MicroServiceConstants.LOGIN_MICROSERVICE;
import static com.cogent.authservice.constants.MicroServiceConstants.LOGIN_MICROSERVICE_KEYCLOAK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.cogent.authservice.security.jwt.JwtConfigurer;
import com.cogent.authservice.security.jwt.JwtTokenProvider;

/**
 * @author smriti on 6/26/19
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .httpBasic().disable()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(LOGIN_MICROSERVICE).permitAll()
                .antMatchers(LOGIN_MICROSERVICE_KEYCLOAK).permitAll()
                .antMatchers("/micro-service/api/sayHello/admin").hasRole("ADMIN")
                .antMatchers("/micro-service/api/sayHello/user").hasRole("USER")
                .antMatchers("/test").hasRole("USER")
                .anyRequest().authenticated()
                .and()
                .apply(new JwtConfigurer(jwtTokenProvider));
    }
}
