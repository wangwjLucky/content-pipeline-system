package com.pipeline.admin.common;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具，用于 API Key 和平台 Cookie 的加密存储。
 *
 * 密钥通过 EncryptionConfig（Spring @Value）或环境变量 AES_ENCRYPTION_KEY 注入。
 * 生产环境务必通过 Spring 配置或环境变量设置 32 字节以上密钥。
 */
@Slf4j
public class EncryptionUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static volatile SecretKey secretKey;

    static {
        initKey();
    }

    private static void initKey() {
        String keyStr = System.getenv("AES_ENCRYPTION_KEY");
        if (keyStr != null && !keyStr.isEmpty()) {
            deriveKey(keyStr);
            log.info("加密密钥已从环境变量 AES_ENCRYPTION_KEY 加载");
            return;
        }
        // 默认密钥（仅用于开发环境！）
        log.warn("⚠ AES_ENCRYPTION_KEY 未设置，使用硬编码默认密钥！生产环境必须通过环境变量或 Spring 配置设置 32 字节以上密钥。");
        deriveKey("pipeline-default-key-change-in-prod!");
    }

    /**
     * 允许运行时动态设置密钥（由 EncryptionConfig @PostConstruct 调用）。
     * 调用后会重新初始化密钥。
     */
    public static void setKey(String keyStr) {
        if (keyStr == null || keyStr.isEmpty()) {
            log.warn("尝试设置空密钥，忽略");
            return;
        }
        deriveKey(keyStr);
        log.info("加密密钥已更新");
    }

    private static void deriveKey(String source) {
        byte[] keyBytes = new byte[32];
        byte[] sourceBytes = source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(sourceBytes, 0, keyBytes, 0, Math.min(sourceBytes.length, 32));
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文，返回 Base64 编码的密文（含 IV）。
     */
    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // IV + 密文 组合编码
            ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherText.length);
            bb.put(iv);
            bb.put(cipherText);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密 Base64 编码的密文（含 IV）。
     */
    public static String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer bb = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            bb.get(iv);
            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }
}
