package com.pipeline.admin.service;

import com.pipeline.admin.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {

    Page<SysUser> list(int page, int size, String status);

    SysUser get(Long id);

    SysUser create(String username, String password, String nickname, Long roleId);

    SysUser update(Long id, String username, String password, String nickname, Long roleId, String status);

    void delete(Long id);
}