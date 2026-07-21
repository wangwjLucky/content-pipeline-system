package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.service.FileService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @OperationLog(module = "文件管理", action = "上传")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam String bucket,
            @RequestParam(defaultValue = "temp") String module,
            @RequestParam(required = false) Long taskId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String key = fileService.upload(bucket, module, taskId, file);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("bucket", bucket);
        data.put("originalName", file.getOriginalFilename());
        data.put("size", file.getSize());
        return Result.success(data);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam String bucket,
            @RequestParam String key) {
        byte[] data = fileService.download(bucket, key);
        String fileName = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(data);
    }

    @GetMapping
    public Result<List<String>> list(
            @RequestParam String bucket,
            @RequestParam(required = false, defaultValue = "") String prefix) {
        return Result.success(fileService.listFiles(bucket, prefix));
    }

    @OperationLog(module = "文件管理", action = "删除")
    @DeleteMapping
    public Result<Void> delete(
            @RequestParam String bucket,
            @RequestParam String key) {
        fileService.delete(bucket, key);
        return Result.success(null);
    }
}