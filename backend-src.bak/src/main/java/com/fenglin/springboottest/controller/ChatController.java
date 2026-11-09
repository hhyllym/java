package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 好友聊天接口，前端 src/chat.js 调的就是这些：
 *   GET  /api/chat/conversations?meId=      会话列表（含最后一条消息、未读数）
 *   GET  /api/chat/history?meId=&friendId=  聊天记录（正序）
 *   POST /api/chat/send   { fromId, toId, content, msgType }  发消息（text/image）
 *   POST /api/chat/recall { meId, msgId }  撤回自己的消息（5 分钟内）
 *   POST /api/chat/call   { fromId, toId, duration, answered }  保存通话记录
 *   POST /api/chat/read   { meId, friendId }  标记已读
 *   GET  /api/chat/unread?meId=              未读总数
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/conversations")
    public Result<?> conversations(@RequestParam Long meId) {
        return chatService.conversations(meId);
    }

    @GetMapping("/history")
    public Result<?> history(@RequestParam Long meId, @RequestParam Long friendId) {
        return chatService.history(meId, friendId);
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody Map<String, Object> body) {
        return chatService.send(toLong(body.get("fromId")), toLong(body.get("toId")),
                (String) body.get("content"), (String) body.get("msgType"));
    }

    @PostMapping("/recall")
    public Result<?> recall(@RequestBody Map<String, Object> body) {
        return chatService.recall(toLong(body.get("meId")), toLong(body.get("msgId")));
    }

    @PostMapping("/call")
    public Result<?> call(@RequestBody Map<String, Object> body) {
        int duration = 0;
        try {
            duration = Integer.parseInt(String.valueOf(body.get("duration")));
        } catch (Exception ignore) {
        }
        boolean answered = !"false".equals(String.valueOf(body.get("answered")));
        return chatService.callLog(toLong(body.get("fromId")), toLong(body.get("toId")),
                duration, answered);
    }

    @PostMapping("/read")
    public Result<?> read(@RequestBody Map<String, Object> body) {
        return chatService.markRead(toLong(body.get("meId")), toLong(body.get("friendId")));
    }

    @GetMapping("/unread")
    public Result<?> unread(@RequestParam Long meId) {
        return Result.ok(chatService.unreadTotal(meId));
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
