package com.fenglin.springboottest.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 密码加盐哈希工具。
 * 规则：t_user.password = SHA-256(salt + 明文)，salt 是每个用户随机 16 位 hex。
 * 和 MySQL 里 SHA2(CONCAT(salt, pwd), 256) 的结果完全一致，方便直接用 SQL 排查。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    /** 生成 16 位十六进制随机盐 */
    public static String randomSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    /** SHA-256(salt + 明文)，返回 64 位小写 hex */
    public static String hash(String salt, String rawPassword) {
        String source = (salt == null ? "" : salt) + (rawPassword == null ? "" : rawPassword);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    /** 校验明文密码和库里存的哈希是否匹配 */
    public static boolean matches(String salt, String rawPassword, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        return hash(salt, rawPassword).equalsIgnoreCase(storedHash);
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX[v >>> 4];
            chars[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(chars);
    }
}
