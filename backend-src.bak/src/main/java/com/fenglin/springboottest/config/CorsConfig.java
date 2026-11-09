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
 * 大富翁四人联机时，其他同学的电脑/手机会用「主机局域网 IP:5180」访问前端，
 * Origin 就变成 http://192.168.x.x:5180 这类地址，所以这里额外放行局域网与
 * 泛域名来源。allowedOriginPatterns 会回显具体 Origin（不是字面 "*"），
 * 因此配合 allowCredentials(true) 带 JSESSIONID 也是安全的。
 * 注意：仅限本地 / 局域网开发环境使用，部署到公网时请收紧来源。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        // 同一 WiFi 下别的设备：局域网 IP / 主机名 / 公网发布页
                        "http://192.168.*",
                        "http://10.*",
                        "http://172.*",
                        "http://*",
                        "https://*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
