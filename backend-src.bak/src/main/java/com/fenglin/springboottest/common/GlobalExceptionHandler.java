package com.fenglin.springboottest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 兜底异常处理：后端报错时也返回 { code, msg, data } 的 JSON，
 * 而不是 Spring 默认的 HTML 错误页 —— 前端 src/auth.js 只认 JSON。
 */
@RestControllerAdvice(basePackages = "com.fenglin.springboottest.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        log.error("接口出错", e);
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = e.getClass().getSimpleName();
        }
        return Result.fail("服务器出错了：" + msg + "（详细堆栈看后端窗口 / log/xx.log）");
    }
}
