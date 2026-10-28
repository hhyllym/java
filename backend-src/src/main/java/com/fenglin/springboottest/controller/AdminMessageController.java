package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.mapper.MessageMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 消息管理（t_message：留言板 / 私信 / 系统通知）。
 *   GET    /api/admin/message/list  分页列表（可按 type 过滤、按内容搜）
 *   DELETE /api/admin/message/{id}  删除一条消息
 */
@RestController
@RequestMapping("/api/admin/message")
public class AdminMessageController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private MessageMapper messageMapper;

    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request,
                        @RequestParam(defaultValue = "") String type,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "12") int size) {
        Result<?> a = auth(request);
        if (a != null) return a;
        String kw = keyword == null ? "" : keyword.trim();
        page = Math.max(1, page);
        size = Math.max(1, Math.min(50, size));

        LambdaQueryWrapper<Message> countW = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) countW.eq(Message::getType, type);
        if (!kw.isEmpty()) countW.like(Message::getContent, kw);
        long total = messageMapper.selectCount(countW);

        LambdaQueryWrapper<Message> listW = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) listW.eq(Message::getType, type);
        if (!kw.isEmpty()) listW.like(Message::getContent, kw);
        listW.orderByDesc(Message::getCreateTime).orderByDesc(Message::getId)
                .last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<Message> rows = messageMapper.selectList(listW);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", rows);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", (total + size - 1) / Math.max(1, size));
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable("id") Long id) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (id == null) return Result.fail("缺少消息 id");
        Message m = messageMapper.selectById(id);
        if (m == null) return Result.fail("找不到这条消息");
        messageMapper.deleteById(id);
        return Result.ok("已删除该消息", null);
    }
}
