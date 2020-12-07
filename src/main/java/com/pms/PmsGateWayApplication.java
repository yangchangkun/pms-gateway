package com.pms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动程序
 * 
 * @author yangchangkun
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
@MapperScan({"com.pms.core.*.mapper", "com.pms.*.mapper"})
@EnableCaching
@EnableAsync //开启异步任务
public class PmsGateWayApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(PmsGateWayApplication.class, args);
        System.out.println("===========================================================================\n");
        System.out.println("====================PmsGateWayApplication 系统启动成功====================\n");
        System.out.println("===========================================================================\n");
    }
}