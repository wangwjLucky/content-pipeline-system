package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.config.JwtUtil;
import com.pipeline.admin.dto.LoginRequest;
import com.pipeline.admin.entity.SysUser;
import com.pipeline.admin.mapper.UserMapper;
import com.pipeline.admin.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest body) {
        String username = body.getUsername();
        String password = body.getPassword();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Result.success(Map.of("token", token, "nickname", user.getNickname()));
    }

    @GetMapping("/me")
    public Result<?> me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Result.error(401, "未认证");
        }
        Object principal = auth.getPrincipal();
        Long userId;
        if (principal instanceof Long) {
            userId = (Long) principal;
        } else if (principal instanceof String s) {
            try {
                userId = Long.valueOf(s);
            } catch (NumberFormatException e) {
                return Result.error(401, "无法解析用户身份：令牌格式无效");
            }
        } else {
            return Result.error(401, "无法解析用户身份");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        return Result.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "roleId", user.getRoleId()
        ));
    }
}