package com.pipeline.admin.common;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring 配置驱动的加密密钥初始化器。
 *
 * 在应用启动时将 Spring Environment 中的 aes.encryption-key 注入到 EncryptionUtil。
 * Docker 部署时通过环境变量 AES_ENCRYPTION_KEY 设置，或自行注入 spring 配置。
 */
@Slf4j
@Component
public class EncryptionConfig {

    @Value("${aes.encryption-key:}")
    private String aesKeyFromConfig;

    @PostConstruct
    public void init() {
        if (aesKeyFromConfig != null && !aesKeyFromConfig.isEmpty()) {
            EncryptionUtil.setKey(aesKeyFromConfig);
            log.info("加密密钥已通过 Spring 配置注入");
        } else {
            log.warn("未配置 aes.encryption-key（环境变量 AES_ENCRYPTION_KEY），使用默认密钥！生产环境务必更换。");
        }
    }
}
