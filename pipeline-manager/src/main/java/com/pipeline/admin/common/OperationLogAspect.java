package com.pipeline.admin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j(topic = "OPERATION_LOG")
@Aspect
@Component
public class OperationLogAspect {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String operator = getCurrentUsername();
        String module = operationLog.module();
        String action = operationLog.action();
        String params = "";
        if (operationLog.logParams()) {
            try {
                params = mapper.writeValueAsString(joinPoint.getArgs());
            } catch (Exception e) {
                params = Arrays.toString(joinPoint.getArgs());
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[{}] 操作人={} | 模块={} | 动作={} | 耗时={}ms | 参数={} | 成功",
                    LocalDateTime.now(), operator, module, action, cost, params);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[{}] 操作人={} | 模块={} | 动作={} | 耗时={}ms | 参数={} | 失败: {}",
                    LocalDateTime.now(), operator, module, action, cost, params, e.getMessage());
            throw e;
        }
        return result;
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}