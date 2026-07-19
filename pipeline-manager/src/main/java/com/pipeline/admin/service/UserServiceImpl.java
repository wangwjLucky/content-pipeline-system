package com.pipeline.admin.service;

import com.pipeline.admin.entity.SysUser;
import com.pipeline.admin.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<SysUser> list(int page, int size, String status) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .eq(status != null && !status.isEmpty(), SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public SysUser get(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public SysUser create(String username, String password, String nickname, Long roleId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRoleId(roleId);
        user.setStatus("ENABLED");
        userMapper.insert(user);
        return user;
    }

    @Override
    public SysUser update(Long id, String username, String password, String nickname, Long roleId, String status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("用户不存在: " + id);

        if (username != null) user.setUsername(username);
        if (password != null && !password.isEmpty()) user.setPassword(passwordEncoder.encode(password));
        if (nickname != null) user.setNickname(nickname);
        if (roleId != null) user.setRoleId(roleId);
        if (status != null) user.setStatus(status);
        user.setId(id);
        userMapper.updateById(user);
        return userMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
    }
}