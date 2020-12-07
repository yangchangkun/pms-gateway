package com.pms.gateway.config;

import com.pms.gateway.interceptor.GatewayInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 网关拦截器配置
 * 注意这里只拦截controller，不拦截静态资源
 * @author yangchangkun
 */
@Configuration
public class GatewayInterceptorConfig implements   WebMvcConfigurer
{

    private static List<String> exclude_Path = Arrays.asList("/app/common/previewImg/**",
            "/app/common/qr/**",
            "/app/common/download/**",
            "/app/common/act/**",
            "/app/common/init/**",
            "/resource/**"
    );

    //以这种方式将拦截器注入为一个bean，可以防止拦截器中无法注入bean的问题出现
    @Bean
    public GatewayInterceptor apiInterceptor(){
        return new GatewayInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(apiInterceptor())
                .addPathPatterns("/app/**")
                .excludePathPatterns(exclude_Path);
    }


}