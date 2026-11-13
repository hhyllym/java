package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.HallService;
import com.fenglin.springboottest.service.impl.HallServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 🏠 星糖大厅接口（所有用户都能进）。
 * 前端 src/hall.js 调的就是这些：
 *   GET  /api/hall/messages?limit=            大厅聊天记录
 *   POST /api/hall/messages  { userId, content }   发消息
 *   GET  /api/hall/online                      在线的小马
 *   GET  /api/hall/rooms                       游戏邀请列表
 *   POST /api/hall/rooms     { userId, game, title, maxMembers }  发起邀请
 *   POST /api/hall/rooms/join   { userId, roomId }
 *   POST /api/hall/rooms/leave  { userId, roomId }
 *   POST /api/hall/rooms/close  { userId, roomId }
 */
@RestController
@RequestMapping("/api/hall")
public class HallController {

    @Autowired
    private HallService hallService;

    @GetMapping("/messages")
    public Result<?> messages(@RequestParam(required = false) Integer limit) {
        return guard(() -> hallService.messages(limit == null ? 100 : limit));
    }

    @PostMapping("/messages")
    public Result<?> send(@RequestBody Map<String, Object> body) {
        return guard(() -> hallService.send(toLong(body.get("userId")), str(body.get("content"))));
    }

    @GetMapping("/online")
    public Result<?> online() {
        return guard(() -> hallService.online());
    }

    @GetMapping("/rooms")
    public Result<?> rooms() {
        return guard(() -> hallService.rooms());
    }

    @PostMapping("/rooms")
    public Result<?> createRoom(@RequestBody Map<String, Object> body) {
        return guard(() -> hallService.createRoom(
                toLong(body.get("userId")), str(body.get("game")), str(body.get("title")),
                body.get("maxMembers") == null ? null : toInt(body.get("maxMembers"), 4)));
    }

    @PostMapping("/rooms/join")
    public Result<?> joinRoom(@RequestBody Map<String, Object> body) {
        return guard(() -> hallService.joinRoom(toLong(body.get("userId")), str(body.get("roomId"))));
    }

    @PostMapping("/rooms/leave")
    public Result<?> leaveRoom(@RequestBody Map<String, Object> body) {
        return guard(() -> hallService.leaveRoom(toLong(body.get("userId")), str(body.get("roomId"))));
    }

    @PostMapping("/rooms/close")
    public Result<?> closeRoom(@RequestBody Map<String, Object> body) {
        return guard(() -> hallService.closeRoom(toLong(body.get("userId")), str(body.get("roomId"))));
    }

    /** 业务异常转成前端能识别的 { code:400, msg }；正常结果直接透传，不重复套一层 */
    private Result<?> guard(Supplier<Result<?>> call) {
        try {
            return call.get();
        } catch (HallServiceImpl.HallException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("大厅服务异常：" + e.getMessage());
        }
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return def;
        }
    }
}
