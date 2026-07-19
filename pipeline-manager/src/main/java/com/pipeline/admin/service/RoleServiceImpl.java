package com.pipeline.admin.service;

import com.pipeline.admin.entity.SysRole;
import com.pipeline.admin.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final SysRoleMapper sysRoleMapper;

    @Override
    public Page<SysRole> list(int page, int size) {
        return sysRoleMapper.selectPage(new Page<>(page, size), null);
    }

    @Override
    public SysRole get(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Override
    public SysRole create(SysRole role) {
        sysRoleMapper.insert(role);
        return role;
    }

    @Override
    public SysRole update(Long id, SysRole role) {
        role.setId(id);
        sysRoleMapper.updateById(role);
        return sysRoleMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        sysRoleMapper.deleteById(id);
    }
}