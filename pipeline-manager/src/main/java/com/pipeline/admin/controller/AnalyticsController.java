package com.pipeline.admin.controller;

import com.pipeline.admin.common.Result;
import com.pipeline.admin.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(analyticsService.getOverview());
    }

    @GetMapping("/daily")
    public Result<Map<String, Object>> daily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(analyticsService.getDaily(startDate, endDate));
    }

    @GetMapping("/topics")
    public Result<Map<String, Object>> topics(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analyticsService.getTopics(limit));
    }

    @GetMapping("/accounts")
    public Result<Map<String, Object>> accounts() {
        return Result.success(analyticsService.getAccounts());
    }
}