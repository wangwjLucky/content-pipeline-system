package com.pipeline.admin.controller;

import com.pipeline.admin.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/v1/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}