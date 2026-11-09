package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消息中心接口，前端 src/message.js 调的就是这些：
 *   留言板：POST /api/message/board  { fromId, fromName, content }
 *           GET  /api/message/board?page=&size=
 *   私信：  POST /api/message/pm      { fromId, fromName, toName, content }
 *           GET  /api/message/pm?box=inbox|sent&userId=&page=&size=
 *           POST /api/message/pm/read { id, userId }
 *   通知：  GET  /api/message/notify?userId=&page=&size=
 *           POST /api/message/notify/read { id, userId }
 *   未读：  GET  /api/message/unread?userId=
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/board")
    public Result<?> postBoard(@RequestBody Map<String, Object> body) {
        return messageService.postBoard(toLong(body.get("fromId")), (String) body.get("fromName"),
                (String) body.get("content"));
    }

    @GetMapping("/board")
    public Result<?> boardList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.boardList(page, size));
    }

    @PostMapping("/pm")
    public Result<?> sendPm(@RequestBody Map<String, Object> body) {
        return messageService.sendPm(toLong(body.get("fromId")), (String) body.get("fromName"),
                (String) body.get("toName"), (String) body.get("content"));
    }

    @GetMapping("/pm")
    public Result<?> pmList(@RequestParam(defaultValue = "inbox") String box,
                            @RequestParam Long userId,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "15") int size) {
        return Result.ok(messageService.pmList(box, userId, page, size));
    }

    @PostMapping("/pm/read")
    public Result<?> readPm(@RequestBody Map<String, Object> body) {
        return messageService.readPm(toLong(body.get("id")), toLong(body.get("userId")));
    }

    @GetMapping("/notify")
    public Result<?> notifyList(@RequestParam Long userId,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "15") int size) {
        return Result.ok(messageService.notifyList(userId, page, size));
    }

    @PostMapping("/notify/read")
    public Result<?> readNotify(@RequestBody Map<String, Object> body) {
        return messageService.readNotify(toLong(body.get("id")), toLong(body.get("userId")));
    }

    @GetMapping("/unread")
    public Result<?> unread(@RequestParam Long userId) {
        return Result.ok(messageService.unread(userId));
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
