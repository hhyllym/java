package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 好友在线状态接口（内存实现，配合前端 src/presence.js）：
 *   POST /api/presence/heartbeat { userId }          上报心跳（登录后每 30 秒）
 *   POST /api/presence/offline   { userId }          主动下线（退出 / 切换账号）
 *   GET  /api/presence/status?ids=1,2,3              批量查询 { userId: 是否在线 }
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    @Autowired
    private PresenceService presenceService;

    @PostMapping("/heartbeat")
    public Result<?> heartbeat(@RequestBody Map<String, Object> body) {
        presenceService.beat(toLong(body.get("userId")));
        return Result.ok(null);
    }

    @PostMapping("/offline")
    public Result<?> offline(@RequestBody Map<String, Object> body) {
        presenceService.offline(toLong(body.get("userId")));
        return Result.ok(null);
    }

    @GetMapping("/status")
    public Result<?> status(@RequestParam String ids) {
        List<Long> idList = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            for (String s : ids.split(",")) {
                Long v = toLong(s.trim());
                if (v != null) {
                    idList.add(v);
                }
            }
        }
        Map<Long, Boolean> data = presenceService.status(idList);
        return Result.ok(data);
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
