package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.PlatformAccount;
import com.pipeline.admin.service.PlatformAccountService;
import com.pipeline.admin.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform-accounts")
@RequiredArgsConstructor
public class PlatformAccountController {
    private final PlatformAccountService platformAccountService;

    @GetMapping
    public Result<Page<PlatformAccount>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String platform) {
        return Result.success(platformAccountService.list(page, size, platform));
    }

    @GetMapping("/{id}")
    public Result<PlatformAccount> get(@PathVariable Long id) {
        PlatformAccount account = platformAccountService.get(id);
        return account != null ? Result.success(account) : Result.error(404, "账号不存在");
    }

    @OperationLog(module = "平台账号", action = "创建")
    @PostMapping
    public Result<PlatformAccount> create(@RequestBody PlatformAccount account) {
        return Result.success(platformAccountService.create(account));
    }

    @OperationLog(module = "平台账号", action = "更新")
    @PutMapping("/{id}")
    public Result<PlatformAccount> update(@PathVariable Long id, @RequestBody PlatformAccount account) {
        return Result.success(platformAccountService.update(id, account));
    }

    @OperationLog(module = "平台账号", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        platformAccountService.delete(id);
        return Result.success(null);
    }
}