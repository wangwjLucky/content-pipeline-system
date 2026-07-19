package com.pipeline.admin.common;

import java.lang.annotation.*;

/**
 * 操作日志注解，标注在需要记录操作日志的 Controller 方法上。
 * 符合技术文档 10.1 节"敏感操作记录操作日志"的要求。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块 */
    String module();

    /** 操作描述 */
    String action();

    /** 是否记录请求参数 */
    boolean logParams() default true;
}