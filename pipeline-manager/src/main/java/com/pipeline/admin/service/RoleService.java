package com.pipeline.admin.service;

import com.pipeline.admin.entity.SysRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface RoleService {

    Page<SysRole> list(int page, int size);

    SysRole get(Long id);

    SysRole create(SysRole role);

    SysRole update(Long id, SysRole role);

    void delete(Long id);
}