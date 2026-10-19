package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.FriendRemark;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.FriendRemarkMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private FriendRemarkMapper friendRemarkMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<?> search(Long meId, String keyword) {
        if (meId == null) {
            return Result.fail("先登录再来加好友吧");
        }
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Result.fail("请输入小马名字或 ID");
        }
        if (kw.length() > 20) {
            kw = kw.substring(0, 20);
        }

        // 名字模糊匹配；纯数字时同时按 ID 精确匹配
        List<User> users = new ArrayList<>();
        List<User> byName = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getName, kw).last("limit 20"));
        users.addAll(byName);
        Long id = tryParseLong(kw);
        if (id != null) {
            User byId = userMapper.selectById(id);
            if (byId != null && users.stream().noneMatch(u -> u.getId().equals(byId.getId()))) {
                users.add(byId);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : users) {
            if (u.getId().equals(meId)) {
                continue; // 不搜自己
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("relation", relationOf(meId, u.getId()));
            out.add(item);
        }
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> apply(Long fromId, Long toId, String msg) {
        if (fromId == null || toId == null) {
            return Result.fail("参数不完整");
        }
        if (fromId.equals(toId)) {
            return Result.fail("不能加自己为好友哦");
        }
        User target = userMapper.selectById(toId);
        if (target == null) {
            return Result.fail("没有找到这只小马，检查一下名字或 ID");
        }

        String relation = relationOf(fromId, toId);
        if ("friend".equals(relation)) {
            return Result.fail("你们已经是好友啦，去聊天吧");
        }
        if ("pending-out".equals(relation)) {
            return Result.fail("申请已经发出去了，耐心等等对方同意");
        }
        if ("pending-in".equals(relation)) {
            return Result.fail("对方已经给你发过申请，去「新的朋友」里处理吧");
        }

        // 曾经是好友（软删记录）：恢复原记录，聊天记录和成为好友的日期都保留
        Friend old = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 2)
                .and(w -> w.eq(Friend::getUserId, fromId).eq(Friend::getFriendId, toId)
                        .or().eq(Friend::getUserId, toId).eq(Friend::getFriendId, fromId))
                .last("limit 1"));
        if (old != null) {
            old.setUserId(fromId);
            old.setFriendId(toId);
            old.setStatus(0);
            old.setApplyMsg(msg == null ? "" : msg.trim().isEmpty() ? "say hi!" : msg.trim());
            old.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(old);
            return Result.ok("好友申请已发送（你们曾经是好友，聊天记录都还在）");
        }

        Friend f = new Friend();
        f.setUserId(fromId);
        f.setFriendId(toId);
        f.setStatus(0);
        f.setApplyMsg(msg == null ? "" : msg.trim().isEmpty() ? "say hi!" : msg.trim());
        f.setCreateTime(LocalDateTime.now());
        f.setUpdateTime(LocalDateTime.now());
        friendMapper.insert(f);
        return Result.ok("好友申请已发送");
    }

    @Override
    public Result<?> applies(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, meId)
                .eq(Friend::getStatus, 0)
                .orderByDesc(Friend::getId));

        List<Map<String, Object>> out = new ArrayList<>();
        for (Friend f : rows) {
            User u = userMapper.selectById(f.getUserId());
            if (u == null) {
                continue; // 申请人账号不在了，跳过
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", f.getId());
            item.put("userId", u.getId());
            item.put("name", u.getName());
            item.put("applyMsg", f.getApplyMsg() == null ? "" : f.getApplyMsg());
            item.put("time", formatTime(f.getCreateTime()));
            out.add(item);
        }
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> handle(Long applyId, Long meId, boolean accept) {
        if (applyId == null || meId == null) {
            return Result.fail("参数不完整");
        }
        Friend f = friendMapper.selectById(applyId);
        if (f == null || !meId.equals(f.getFriendId()) || f.getStatus() == null || f.getStatus() != 0) {
            return Result.fail("这条申请不存在或已经处理过了");
        }
        if (accept) {
            f.setStatus(1);
            f.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(f);
            return Result.ok("已同意，你们是好友啦");
        }
        friendMapper.deleteById(applyId);
        return Result.ok("已拒绝");
    }

    @Override
    public Result<?> list(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, meId).or().eq(Friend::getFriendId, meId))
                .orderByDesc(Friend::getUpdateTime));

        List<Map<String, Object>> out = new ArrayList<>();
        // 批量查「我对这些好友的备注」：每人独立一份，互不可见
        Map<Long, String> remarkOf = new LinkedHashMap<>();
        List<Long> otherIds = rows.stream()
                .map(f -> meId.equals(f.getUserId()) ? f.getFriendId() : f.getUserId())
                .distinct().collect(Collectors.toList());
        if (!otherIds.isEmpty()) {
            List<FriendRemark> remarks = friendRemarkMapper.selectList(new LambdaQueryWrapper<FriendRemark>()
                    .eq(FriendRemark::getMeId, meId)
                    .in(FriendRemark::getFriendId, otherIds));
            for (FriendRemark r : remarks) {
                remarkOf.put(r.getFriendId(), r.getRemark() == null ? "" : r.getRemark());
            }
        }
        for (Friend f : rows) {
            Long otherId = meId.equals(f.getUserId()) ? f.getFriendId() : f.getUserId();
            User u = userMapper.selectById(otherId);
            if (u == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
            item.put("since", formatTime(f.getCreateTime()));
            item.put("relationType", f.getRelationType() == null ? "" : f.getRelationType());
            item.put("remark", remarkOf.getOrDefault(u.getId(), ""));
            out.add(item);
        }
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(Long meId, Long friendId) {
        if (meId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        // 软删除（status=2）：保留记录，再加回时恢复原关系与聊天记录
        friendMapper.update(null, new LambdaUpdateWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, meId))
                .set(Friend::getStatus, 2)
                .set(Friend::getUpdateTime, LocalDateTime.now()));
        return Result.ok("已解除好友关系（聊天记录已保留，再加回即可续聊）");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> setRemark(Long meId, Long friendId, String remark) {
        if (meId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        String text = remark == null ? "" : remark.trim();
        if (text.length() > 32) {
            text = text.substring(0, 32);
        }
        FriendRemark row = friendRemarkMapper.selectOne(new LambdaQueryWrapper<FriendRemark>()
                .eq(FriendRemark::getMeId, meId)
                .eq(FriendRemark::getFriendId, friendId)
                .last("limit 1"));
        if (text.isEmpty()) {
            // 清空备注：删掉这行，下次列表自然不带备注
            if (row != null) {
                friendRemarkMapper.deleteById(row.getId());
            }
            return Result.ok("备注已清除");
        }
        if (row == null) {
            row = new FriendRemark();
            row.setMeId(meId);
            row.setFriendId(friendId);
            row.setRemark(text);
            row.setUpdateTime(LocalDateTime.now());
            friendRemarkMapper.insert(row);
        } else {
            row.setRemark(text);
            row.setUpdateTime(LocalDateTime.now());
            friendRemarkMapper.updateById(row);
        }
        return Result.ok("备注已保存");
    }

    /** meId 与对方的关系：none / pending-out / pending-in / friend */
    private String relationOf(Long meId, Long otherId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, otherId)
                        .or().eq(Friend::getUserId, otherId).eq(Friend::getFriendId, meId)));
        boolean hasFriend = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == 1);
        if (hasFriend) {
            return "friend";
        }
        boolean myApply = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == 0
                && meId.equals(f.getUserId()));
        if (myApply) {
            return "pending-out";
        }
        boolean theirApply = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == 0
                && otherId.equals(f.getUserId()));
        if (theirApply) {
            return "pending-in";
        }
        return "none";
    }

    private Long tryParseLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTime(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        if (t.toLocalDate().equals(now.toLocalDate())) {
            return t.format(hm);
        }
        return t.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }
}
