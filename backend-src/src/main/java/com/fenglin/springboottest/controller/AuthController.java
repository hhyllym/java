package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.CaptchaService;
import com.fenglin.springboottest.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 小马王国登录 / 注册接口，前端 src/auth.js 调的就是这三个：
 *   GET  /api/auth/captcha   拿验证码 PNG（答案存在 session 里）
 *   POST /api/auth/register  { name, password }
 *   POST /api/auth/login     { name, password, captcha }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public void captcha(HttpSession session, HttpServletResponse response) throws IOException {
        BufferedImage image = captchaService.generate(session);
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        try (OutputStream out = response.getOutputStream()) {
            ImageIO.write(image, "png", out);
            out.flush();
        }
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String password = body.get("password");
        log.info("注册请求 name={}", name);
        return userService.register(name, password);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String name = body.get("name");
        String password = body.get("password");
        String captcha = body.get("captcha");

        // 验证码先校验，而且用完即废（不管对错）
        if (!captchaService.verify(session, captcha)) {
            log.info("登录失败：验证码错误 name={}", name);
            return Result.fail("验证码不正确，请重新输入");
        }

        Result<?> result = userService.login(name, password);
        log.info("登录结果 name={} code={} msg={}", name, result.getCode(), result.getMsg());
        return result;
    }
}
