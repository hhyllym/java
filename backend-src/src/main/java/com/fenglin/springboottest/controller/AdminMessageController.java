package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.MessageMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 消息管理（t_message：留言板 / 私信 / 系统通知）。
 *   GET    /api/admin/message/list  分页列表（可按 type 过滤、按内容搜）
 *   POST   /api/admin/message/send  发系统通知（NOTIFY），收件人留空 = 全员广播
 *   DELETE /api/admin/message/{id}  删除一条消息
 */
@RestController
@RequestMapping("/api/admin/message")
public class AdminMessageController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;

    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    /** 取当前登录管理员（发通知时当作发送人） */
    private User currentAdmin(HttpServletRequest request) {
        String name = adminAuth.nameOf(request.getHeader(AdminAuth.HEADER));
        if (name == null) return null;
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
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

    /**
     * 发系统通知（type=NOTIFY）。
     *   { "toName": "", "content": "..." }  toName 留空 = 全员广播（toId=-1）
     *   指定 toName 时按小马名字找收件人，找不到直接报错，不会静默丢。
     */
    @PostMapping("/send")
    public Result<?> send(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        String toName = body == null ? null : body.get("toName");
        String content = body == null ? null : body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.fail("通知内容不能为空");
        }
        content = content.trim();
        if (content.length() > 500) {
            return Result.fail("通知内容太长了（最多 500 字）");
        }

        User me = currentAdmin(request);
        String fromName = me == null ? "系统" : me.getName();
        Long fromId = me == null ? null : me.getId();

        Message m = new Message();
        m.setType("NOTIFY");
        m.setFromId(fromId);
        m.setFromName(fromName);
        m.setContent(content);
        m.setCreateTime(LocalDateTime.now());

        if (toName == null || toName.trim().isEmpty()) {
            // 全员广播：toId=-1，按 Message 的约定广播始终视为已读
            m.setToId(-1L);
            m.setToName(null);
            m.setReadFlag(1);
        } else {
            String name = toName.trim();
            User to = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
            if (to == null) {
                return Result.fail("没有找到收件小马「" + name + "」，检查名字有没有写对");
            }
            m.setToId(to.getId());
            m.setToName(to.getName());
            m.setReadFlag(0);
        }
        messageMapper.insert(m);

        String scope = m.getToId() != null && m.getToId() == -1L ? "全员广播" : "「" + m.getToName() + "」";
        return Result.ok("系统通知已发送（" + scope + "）", null);
    }
}
