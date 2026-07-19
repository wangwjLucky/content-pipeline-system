package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.SysUser;
import com.pipeline.admin.service.UserService;
import com.pipeline.admin.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public Result<Page<SysUser>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String status) {
        return Result.success(userService.list(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser user = userService.get(id);
        return user != null ? Result.success(user) : Result.error(404, "用户不存在");
    }

    @OperationLog(module = "用户管理", action = "创建")
    @PostMapping
    public Result<SysUser> create(@RequestBody Map<String, Object> body) {
        SysUser user = userService.create(
                (String) body.get("username"),
                (String) body.get("password"),
                (String) body.get("nickname"),
                body.get("roleId") != null ? Long.valueOf(body.get("roleId").toString()) : null
        );
        return Result.success(user);
    }

    @OperationLog(module = "用户管理", action = "更新")
    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(userService.update(
                id,
                (String) body.get("username"),
                (String) body.get("password"),
                (String) body.get("nickname"),
                body.get("roleId") != null ? Long.valueOf(body.get("roleId").toString()) : null,
                (String) body.get("status")
        ));
    }

    @OperationLog(module = "用户管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
}