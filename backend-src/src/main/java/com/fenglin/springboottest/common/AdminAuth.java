package com.fenglin.springboottest.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 后台管理系统「管理员令牌」签发与校验（轻量演示用，不引 JWT）。
 *   - 令牌 = Base64(loginName + "|" + secret)，loginName 即 t_user 的小马名字
 *   - 每个后台接口带请求头 X-Admin-Token，这里核对 secret 段
 * secret 在 application-*.properties 的 app.admin.secret 配置。
 */
@Component
public class AdminAuth {

    /** 前端请求头名字（前端 adminAuth.js 里要一致） */
    public static final String HEADER = "X-Admin-Token";

    private final String secret;

    public AdminAuth(@Value("${app.admin.secret}") String secret) {
        this.secret = secret;
    }

    /** 登录成功时签发令牌 */
    public String issueToken(String loginName) {
        String raw = loginName + "|" + secret;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 校验前端带来的令牌是否合法（能解码、且 secret 段对得上） */
    public boolean isValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            return parts.length == 2 && secret.equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }

    /** 从令牌里取出登录名（/me 接口用） */
    public String nameOf(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            return parts.length == 2 ? parts[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
}
