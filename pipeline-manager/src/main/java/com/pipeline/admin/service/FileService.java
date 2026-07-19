package com.pipeline.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    /**
     * 上传文件到指定 bucket
     * @param bucket 存储桶名称
     * @param module 模块前缀（如 videos-raw / voices）
     * @param taskId 关联任务 ID
     * @param file   上传的文件
     * @return 文件访问 URL
     */
    public String upload(String bucket, String module, Long taskId, MultipartFile file) throws IOException {
        // 生成唯一文件名：{module}/{taskId}/{uuid}_{原始文件名}
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String key = String.format("%s/%d/%s%s", module, taskId, UUID.randomUUID().toString().substring(0, 8), ext);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        log.info("文件上传成功: bucket={}, key={}", bucket, key);
        return key;
    }

    /**
     * 获取文件下载 URL（通过 S3 的 GetObject 返回字节流）
     */
    public byte[] download(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try {
            return s3Client.getObject(request).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败: " + key, e);
        }
    }

    /**
     * 列出 bucket 下的文件
     */
    public List<String> listFiles(String bucket, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * 删除文件
     */
    public void delete(String bucket, String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(request);
        log.info("文件删除成功: bucket={}, key={}", bucket, key);
    }

    /**
     * 确保 bucket 存在，不存在则创建
     */
    public void ensureBucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket 已创建: {}", bucketName);
        }
    }
}