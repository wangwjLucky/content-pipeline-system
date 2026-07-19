package com.pipeline.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("内容生产流水线系统 API")
                        .version("1.0.0")
                        .description("内容生产流水线系统后端接口文档 — 管理选题、脚本、分镜、素材、配音、剪辑、发布全流程")
                        .contact(new Contact().name("Pipeline Team"))
                        .license(new License().name("Apache 2.0")));
    }
}