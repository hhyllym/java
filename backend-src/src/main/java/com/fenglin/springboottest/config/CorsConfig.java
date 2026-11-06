package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开发期跨域放行。
 * 前端本来走 Vite 的 /api 代理，但代理会把浏览器的 Origin 头一起转发给后端，
 * 所以后端仍会做 CORS 校验 —— 前端端口一变（5173 → 5180 → 以后可能再变）就会
 * 直接 403 "Invalid CORS request"，连登录逻辑都走不到。
 *
 * 因此这里用 allowedOriginPatterns 按端口通配 localhost / 127.0.0.1，
 * 避免每次换端口都要改后端。仅限本机开发环境使用。
 * 注意：因为要带 JSESSIONID（验证码靠 session），所以不能用 "*"。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
