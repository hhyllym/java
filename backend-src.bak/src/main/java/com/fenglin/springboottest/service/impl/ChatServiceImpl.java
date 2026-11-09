package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.ChatMessage;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.ChatMessageMapper;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    /** 一次最多带回来的历史消息条数 */
    private static final int HISTORY_LIMIT = 200;
    /** 会话列表汇总时最多扫描的消息条数 */
    private static final int SCAN_LIMIT = 500;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<?> conversations(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<Map<String, Object>> friends = friendList(meId);

        // 扫描与我相关的最近消息：按 id 倒序，第一条即每个好友的「最新消息」
        List<ChatMessage> recent = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromId, meId).or().eq(ChatMessage::getToId, meId))
                .orderByDesc(ChatMessage::getId)
                .last("limit " + SCAN_LIMIT));

        Map<Long, ChatMessage> lastOf = new HashMap<>();
        Map<Long, Integer> unreadOf = new HashMap<>();
        for (ChatMessage m : recent) {
            Long otherId = meId.equals(m.getFromId()) ? m.getToId() : m.getFromId();
            lastOf.putIfAbsent(otherId, m);
            if (meId.equals(m.getToId()) && m.getReadFlag() != null && m.getReadFlag() == 0) {
                unreadOf.merge(otherId, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> f : friends) {
            Long fid = (Long) f.get("id");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", fid);
            item.put("name", f.get("name"));
            ChatMessage last = lastOf.get(fid);
            if (last == null) {
                item.put("lastContent", "还没有聊天记录，打个招呼吧～");
                item.put("lastTime", "");
                item.put("lastMine", false);
                item.put("unread", 0);
            } else {
                boolean mine = meId.equals(last.getFromId());
                item.put("lastContent", (mine ? "我：" : "") + brief(last));
                item.put("lastTime", formatTime(last.getCreateTime()));
                item.put("lastMine", mine);
                item.put("unread", unreadOf.getOrDefault(fid, 0));
            }
            out.add(item);
        }
        // 有新消息的会话排前面
        out.sort((a, b) -> Integer.compare((Integer) b.get("unread"), (Integer) a.get("unread")));
        return Result.ok(out);
    }

    @Override
    public Result<?> history(Long meId, Long friendId) {
        if (meId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromId, meId).eq(ChatMessage::getToId, friendId)
                        .or().eq(ChatMessage::getFromId, friendId).eq(ChatMessage::getToId, meId))
                .orderByAsc(ChatMessage::getId)
                .last("limit " + HISTORY_LIMIT));

        List<Map<String, Object>> out = new ArrayList<>();
        for (ChatMessage m : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("mine", meId.equals(m.getFromId()));
            item.put("type", m.getMsgType() == null ? "text" : m.getMsgType());
            item.put("recalled", m.getRecalled() != null && m.getRecalled() == 1);
            item.put("extra", m.getExtra());
            item.put("content", m.getContent());
            item.put("time", formatTime(m.getCreateTime()));
            out.add(item);
        }
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> send(Long fromId, Long toId, String content, String msgType) {
        if (fromId == null || toId == null) {
            return Result.fail("参数不完整");
        }
        String type = (msgType == null || msgType.isEmpty()) ? "text" : msgType;
        if (!"text".equals(type) && !"image".equals(type) && !"audio".equals(type)) {
            return Result.fail("不支持的消息类型");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            return Result.fail("消息不能为空");
        }
        // 图片 / 语音都是 base64 字符串，放宽到 2MB；文本仍限制 500 字
        int maxLen = ("image".equals(type) || "audio".equals(type)) ? 2 * 1024 * 1024 : 500;
        if (text.length() > maxLen) {
            return Result.fail("image".equals(type) ? "图片太大了，换一张小一点的吧" : "消息太长了");
        }
        if (!isFriend(fromId, toId)) {
            return Result.fail("你们还不是好友，先加个好友吧");
        }

        ChatMessage m = new ChatMessage();
        m.setFromId(fromId);
        m.setToId(toId);
        m.setContent(text);
        m.setMsgType(type);
        m.setRecalled(0);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(m);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", m.getId());
        data.put("mine", true);
        data.put("type", type);
        data.put("recalled", false);
        data.put("extra", m.getExtra());
        data.put("content", m.getContent());
        data.put("time", formatTime(m.getCreateTime()));
        return Result.ok("发送成功", data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> recall(Long meId, Long msgId) {
        if (meId == null || msgId == null) {
            return Result.fail("参数不完整");
        }
        ChatMessage m = chatMessageMapper.selectById(msgId);
        if (m == null || !meId.equals(m.getFromId())) {
            return Result.fail("只能撤回自己发的消息");
        }
        if (m.getRecalled() != null && m.getRecalled() == 1) {
            return Result.fail("这条消息已经撤回了");
        }
        if (m.getCreateTime() != null
                && m.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            return Result.fail("超过 5 分钟的消息不能撤回");
        }
        m.setRecalled(1);
        m.setContent("");
        chatMessageMapper.updateById(m);
        return Result.ok("已撤回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> callLog(Long fromId, Long toId, int duration, boolean answered) {
        if (fromId == null || toId == null) {
            return Result.fail("参数不完整");
        }
        if (!isFriend(fromId, toId)) {
            return Result.fail("你们还不是好友，先加个好友吧");
        }
        ChatMessage m = new ChatMessage();
        m.setFromId(fromId);
        m.setToId(toId);
        m.setContent("");
        m.setMsgType("call");
        m.setRecalled(0);
        m.setExtra(String.valueOf(Math.max(0, duration)));
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(m);
        return Result.ok("通话记录已保存");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> markRead(Long meId, Long friendId) {
        if (meId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getFromId, friendId)
                .eq(ChatMessage::getToId, meId)
                .eq(ChatMessage::getReadFlag, 0)
                .set(ChatMessage::getReadFlag, 1));
        return Result.ok(null);
    }

    @Override
    public int unreadTotal(Long meId) {
        if (meId == null) {
            return 0;
        }
        Long cnt = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getToId, meId)
                .eq(ChatMessage::getReadFlag, 0));
        return cnt == null ? 0 : cnt.intValue();
    }

    /** 我的好友列表（id + name） */
    private List<Map<String, Object>> friendList(Long meId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, meId).or().eq(Friend::getFriendId, meId))
                .orderByDesc(Friend::getUpdateTime));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Friend f : rows) {
            Long otherId = meId.equals(f.getUserId()) ? f.getFriendId() : f.getUserId();
            User u = userMapper.selectById(otherId);
            if (u == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            out.add(item);
        }
        return out;
    }

    /** 会话列表里的消息摘要：图片/通话/撤回显示占位文案 */
    private String brief(ChatMessage m) {
        if (m.getRecalled() != null && m.getRecalled() == 1) {
            return "撤回了一条消息";
        }
        String type = m.getMsgType() == null ? "text" : m.getMsgType();
        if ("image".equals(type)) {
            return "[图片]";
        }
        if ("audio".equals(type)) {
            return "[语音]";
        }
        if ("call".equals(type)) {
            int sec = 0;
            try {
                sec = Integer.parseInt(m.getExtra());
            } catch (Exception ignore) {
            }
            return sec > 0 ? "[语音通话 " + fmtDur(sec) + "]" : "[未接来电]";
        }
        return m.getContent();
    }

    /** 秒数格式化成 mm:ss */
    public static String fmtDur(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    private boolean isFriend(Long a, Long b) {
        Long cnt = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, a).eq(Friend::getFriendId, b)
                        .or().eq(Friend::getUserId, b).eq(Friend::getFriendId, a)));
        return cnt != null && cnt > 0;
    }

    private String formatTime(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        if (t.toLocalDate().equals(now.toLocalDate())) {
            return t.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return t.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
}
