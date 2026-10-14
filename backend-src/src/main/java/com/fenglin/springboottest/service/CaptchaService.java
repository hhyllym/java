package com.fenglin.springboottest.service;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

/**
 * 图形验证码：4 位，存 HTTP session，一次性使用（校验完立刻失效）。
 * 前端 <img src="/api/auth/captcha?t=xxx"> 直接拿 PNG，浏览器会自动带上 JSESSIONID，
 * 所以登录时不用额外交验证码 id。
 */
@Service
public class CaptchaService {

    /** 放在 session 里的 key */
    public static final String SESSION_KEY = "MLP_CAPTCHA";

    /** 前端校验也是按 4 位来的，别改 */
    public static final int CODE_LENGTH = 4;

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;

    /** 去掉了 0 O 1 I 这些容易看错的字符 */
    private static final char[] POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final Color[] INK = {
            new Color(0xE0, 0x56, 0x7C),
            new Color(0x7B, 0x5B, 0xD6),
            new Color(0x2E, 0x9B, 0xB5),
            new Color(0xC9, 0x8A, 0x00)
    };

    private final SecureRandom random = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    /** 生成验证码图片，并把答案写进 session */
    public BufferedImage generate(HttpSession session) {
        String code = randomCode();
        session.setAttribute(SESSION_KEY, code);
        // 想在后端窗口看到验证码明文做调试：
        // 把 application.properties 里 com.fenglin.springboottest 的日志级别改成 debug
        log.debug("生成验证码 [{}] sessionId={}", code, session.getId());
        return draw(code);
    }

    /**
     * 校验验证码。一次性：不管对不对，用完就作废。
     * 所以前端失败之后必须换一张新的。
     */
    public boolean verify(HttpSession session, String input) {
        Object saved = session.getAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_KEY);
        if (saved == null || input == null) {
            return false;
        }
        return saved.toString().equalsIgnoreCase(input.trim());
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(POOL[random.nextInt(POOL.length)]);
        }
        return sb.toString();
    }

    private BufferedImage draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景：马卡龙浅粉
            g.setColor(new Color(0xFF, 0xF7, 0xFB));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1.4f));
            for (int i = 0; i < 6; i++) {
                g.setColor(INK[random.nextInt(INK.length)]);
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 干扰点
            for (int i = 0; i < 60; i++) {
                g.setColor(new Color(random.nextInt(0xDDDDDD)));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }

            // 字符
            int step = (WIDTH - 20) / CODE_LENGTH;
            for (int i = 0; i < code.length(); i++) {
                AffineTransform old = g.getTransform();
                double angle = (random.nextDouble() - 0.5) * 0.5;   // 轻微旋转
                int x = 14 + i * step;
                int y = 32 + random.nextInt(6) - 3;
                g.rotate(angle, x, y);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
                g.setColor(INK[i % INK.length]);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
