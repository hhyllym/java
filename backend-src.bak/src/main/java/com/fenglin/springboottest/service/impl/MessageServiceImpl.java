package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.MessageMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    private static final int BOARD_SIZE = 20;
    private static final int PM_SIZE = 15;
    private static final int NOTIFY_SIZE = 15;
    private static final int MAX_CONTENT = 500;
    /** 系统通知 to_id = -1 表示全员广播（公告） */
    private static final Long BROADCAST = -1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<?> postBoard(Long fromId, String fromName, String content) {
        if (fromId == null || fromName == null) {
            return Result.fail("请先登录再留言");
        }
        content = content == null ? "" : content.trim();
        if (content.isEmpty()) {
            return Result.fail("留言内容不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            return Result.fail("留言太长啦，最多 " + MAX_CONTENT + " 字");
        }
        Message m = new Message();
        m.setType("BOARD");
        m.setFromId(fromId);
        m.setFromName(fromName);
        m.setContent(content);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
        return Result.ok("留言成功", toMap(m));
    }

    @Override
    public Map<String, Object> boardList(int page, int size) {
        page = Math.max(1, page);
        size = Math.min(BOARD_SIZE, Math.max(1, size));
        long total = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>().eq(Message::getType, "BOARD"));
        List<Message> rows = total == 0 ? new ArrayList<>()
                : messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .eq(Message::getType, "BOARD")
                    .orderByDesc(Message::getCreateTime)
                    .last("limit " + ((page - 1) * size) + "," + size));
        return pageResult(rows, total, page, size);
    }

    @Override
    public Result<?> sendPm(Long fromId, String fromName, String toName, String content) {
        if (fromId == null || fromName == null) {
            return Result.fail("请先登录再发私信");
        }
        toName = toName == null ? "" : toName.trim();
        if (toName.isEmpty()) {
            return Result.fail("请填写收件人");
        }
        content = content == null ? "" : content.trim();
        if (content.isEmpty()) {
            return Result.fail("私信内容不能为空");
        }
        if (content.length() > MAX_CONTENT) {
            return Result.fail("私信太长啦，最多 " + MAX_CONTENT + " 字");
        }
        User to = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, toName).last("limit 1"));
        if (to == null) {
            return Result.fail("找不到叫「" + toName + "」的小马");
        }
        if (to.getId().equals(fromId)) {
            return Result.fail("不能给自己发私信哦");
        }
        Message m = new Message();
        m.setType("PM");
        m.setFromId(fromId);
        m.setFromName(fromName);
        m.setToId(to.getId());
        m.setToName(to.getName());
        m.setContent(content);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
        return Result.ok("已发送", toMap(m));
    }

    @Override
    public Map<String, Object> pmList(String box, Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(PM_SIZE, Math.max(1, size));
        boolean sent = "sent".equals(box);
        long total = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getType, "PM")
                .eq(sent ? Message::getFromId : Message::getToId, userId));
        List<Message> rows = total == 0 ? new ArrayList<>()
                : messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .eq(Message::getType, "PM")
                    .eq(sent ? Message::getFromId : Message::getToId, userId)
                    .orderByDesc(Message::getCreateTime)
                    .last("limit " + ((page - 1) * size) + "," + size));
        return pageResult(rows, total, page, size);
    }

    @Override
    public Result<?> readPm(Long id, Long userId) {
        if (id == null || userId == null) {
            return Result.fail("参数缺失");
        }
        Message m = messageMapper.selectById(id);
        if (m == null || !"PM".equals(m.getType())) {
            return Result.fail("私信不存在");
        }
        if (!userId.equals(m.getToId())) {
            return Result.fail("只能标记自己收到的私信");
        }
        if (m.getReadFlag() != null && m.getReadFlag() == 1) {
            return Result.ok("已经是已读了");
        }
        Message upd = new Message();
        upd.setId(id);
        upd.setReadFlag(1);
        messageMapper.updateById(upd);
        return Result.ok("已标记为已读");
    }

    @Override
    public Map<String, Object> notifyList(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(NOTIFY_SIZE, Math.max(1, size));
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<Message>()
                .eq(Message::getType, "NOTIFY")
                .and(x -> x.eq(Message::getToId, userId).or().eq(Message::getToId, BROADCAST));
        long total = messageMapper.selectCount(w);
        List<Message> rows = total == 0 ? new ArrayList<>()
                : messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .eq(Message::getType, "NOTIFY")
                    .and(x -> x.eq(Message::getToId, userId).or().eq(Message::getToId, BROADCAST))
                    .orderByDesc(Message::getCreateTime)
                    .last("limit " + ((page - 1) * size) + "," + size));
        return pageResult(rows, total, page, size);
    }

    @Override
    public Result<?> readNotify(Long id, Long userId) {
        Message m = messageMapper.selectById(id);
        if (m == null || !"NOTIFY".equals(m.getType())) {
            return Result.fail("通知不存在");
        }
        // 全员广播公告是共享行，标已读会影响所有人，这里直接当作已读处理（no-op）
        if (BROADCAST.equals(m.getToId())) {
            return Result.ok("已读");
        }
        if (!userId.equals(m.getToId())) {
            return Result.fail("只能标记自己的通知");
        }
        Message upd = new Message();
        upd.setId(id);
        upd.setReadFlag(1);
        messageMapper.updateById(upd);
        return Result.ok("已读");
    }

    @Override
    public Map<String, Object> unread(Long userId) {
        long pm = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getType, "PM")
                .eq(Message::getToId, userId)
                .eq(Message::getReadFlag, 0));
        // 广播公告(-1)不计入未读，避免一条公告让所有人的红点永远亮着
        long notify = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getType, "NOTIFY")
                .eq(Message::getToId, userId)
                .eq(Message::getReadFlag, 0));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pm", pm);
        data.put("notify", notify);
        data.put("total", pm + notify);
        return data;
    }

    // ---------------- helpers ----------------

    private Map<String, Object> pageResult(List<Message> rows, long total, int page, int size) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", toMapList(rows));
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", size <= 0 ? 0 : (int) ((total + size - 1) / size));
        data.put("hasMore", (long) page * size < total);
        return data;
    }

    private Map<String, Object> toMap(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("type", m.getType());
        map.put("fromId", m.getFromId());
        map.put("fromName", m.getFromName());
        map.put("toId", m.getToId());
        map.put("toName", m.getToName());
        map.put("content", m.getContent());
        // 广播公告始终视为已读
        int rf = (m.getReadFlag() == null) ? 0 : m.getReadFlag();
        if (BROADCAST.equals(m.getToId())) {
            rf = 1;
        }
        map.put("readFlag", rf);
        map.put("createTime", m.getCreateTime() == null ? "" : m.getCreateTime().format(FMT));
        return map;
    }

    private List<Map<String, Object>> toMapList(List<Message> list) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (Message m : list) {
            res.add(toMap(m));
        }
        return res;
    }
}
