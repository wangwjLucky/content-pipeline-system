package com.pipeline.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pipeline.admin.mapper")
public class PipelineAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(PipelineAdminApplication.class, args);
    }
}