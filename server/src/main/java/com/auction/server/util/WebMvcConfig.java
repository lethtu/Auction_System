package com.auction.server.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/admin/**", "/api/seller/**", "/api/users/**", "/api/bidder/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/signup",
                        "/api/forgot_pass",
                        "/api/check_code",
                        "/api/files/**"
                );
    }
}
