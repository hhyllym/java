package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 好友在线状态（内存实现，单后端够用；后端重启后所有人会在下次心跳重新上线）。
 * 判断规则：最近 90 秒内有过心跳才算"在线"。
 */
@Service
public class PresenceService {

    /** 超过这个时间没心跳就视为离线（毫秒） */
    private static final long TIMEOUT_MS = 90_000;

    /** userId -> 最近一次心跳时间戳 */
    private final ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();

    /** 上报心跳：登录后前端每 30 秒调一次 */
    public void beat(Long userId) {
        if (userId != null) {
            lastSeen.put(userId, System.currentTimeMillis());
        }
    }

    /** 主动下线：退出登录 / 切换账号时调用 */
    public void offline(Long userId) {
        if (userId != null) {
            lastSeen.remove(userId);
        }
    }

    /** 单个用户是否在线 */
    public boolean isOnline(Long userId) {
        if (userId == null) return false;
        Long t = lastSeen.get(userId);
        return t != null && (System.currentTimeMillis() - t) <= TIMEOUT_MS;
    }

    /** 批量查询在线状态，返回 { userId: 是否在线 } */
    public Map<Long, Boolean> status(List<Long> ids) {
        Map<Long, Boolean> out = new LinkedHashMap<>();
        if (ids != null) {
            for (Long id : ids) {
                out.put(id, isOnline(id));
            }
        }
        return out;
    }
}
