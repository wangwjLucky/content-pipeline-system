package com.pipeline.admin.controller;

import com.pipeline.admin.entity.SysRole;
import com.pipeline.admin.service.RoleService;
import com.pipeline.admin.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping
    public Result<Page<SysRole>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(roleService.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<SysRole> get(@PathVariable Long id) {
        SysRole role = roleService.get(id);
        return role != null ? Result.success(role) : Result.error(404, "角色不存在");
    }

    @PostMapping
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.success(roleService.create(role));
    }

    @PutMapping("/{id}")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        return Result.success(roleService.update(id, role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success(null);
    }
}