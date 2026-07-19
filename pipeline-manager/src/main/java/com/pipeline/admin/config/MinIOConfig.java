package com.pipeline.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * S3 兼容存储配置（支持 MinIO、AWS S3、阿里云 OSS、腾讯 COS 等）。
 * 通过修改 endpoint 即可切换存储服务，无需改动代码。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinIOConfig {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "pipeline";
    private String secretKey = "pipeline123";
    private String region = "us-east-1";

    // Bucket 名称
    private String bucketScripts = "pipeline-scripts";
    private String bucketImages = "pipeline-images";
    private String bucketVideosRaw = "pipeline-videos-raw";
    private String bucketVideosFinal = "pipeline-videos-final";
    private String bucketVoices = "pipeline-voices";
    private String bucketCovers = "pipeline-covers";
    private String bucketTemp = "pipeline-temp";

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }
}