package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.ChatMessage;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.FriendRemark;
import com.fenglin.springboottest.entity.GameScore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.ChatMessageMapper;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.FriendRemarkMapper;
import com.fenglin.springboottest.mapper.GameScoreMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    /** status: 0 申请中 / 1 已是好友 / 2 已解除 / 3 申请未通过（拒绝或撤回后可重发） */
    private static final int ST_PENDING = 0;
    private static final int ST_FRIEND = 1;
    private static final int ST_REMOVED = 2;
    private static final int ST_REJECTED = 3;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private FriendRemarkMapper friendRemarkMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private GameScoreMapper gameScoreMapper;

    @Override
    public Result<?> search(Long meId, String keyword) {
        if (meId == null) {
            return Result.fail("先登录再来加好友吧");
        }
        String kw = keyword == null ? "" : keyword.trim();
        // like 会拼成 %kw% ，用户输入 % 或 _ 会变成通配符，先滤掉避免「搜出全站的人」
        kw = kw.replace("%", "").replace("_", "").trim();
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
            item.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
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
            return Result.fail("申请已经发出去了，耐心等等对方同意；也可以在「新的朋友」里撤回");
        }
        if ("pending-in".equals(relation)) {
            return Result.fail("对方已经给你发过申请，去「新的朋友」里处理吧");
        }

        String applyText = normalizeApplyMsg(msg);

        // 曾经是好友 / 申请被拒：复用原记录，避免撞 (user_id, friend_id) 唯一键，
        // 同时保留聊天记录和最初成为好友的日期。
        Friend old = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .in(Friend::getStatus, Arrays.asList(ST_REMOVED, ST_REJECTED))
                .and(w -> w.eq(Friend::getUserId, fromId).eq(Friend::getFriendId, toId)
                        .or().eq(Friend::getUserId, toId).eq(Friend::getFriendId, fromId))
                .last("limit 1"));
        if (old != null) {
            boolean wasFriend = old.getStatus() != null && old.getStatus() == ST_REMOVED;
            old.setUserId(fromId);
            old.setFriendId(toId);
            old.setStatus(ST_PENDING);
            old.setApplyMsg(applyText);
            if (!wasFriend) {
                old.setCreateTime(LocalDateTime.now());
            }
            old.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(old);
            return Result.ok(wasFriend
                    ? "好友申请已发送（你们曾经是好友，聊天记录都还在）"
                    : "好友申请已重新发送");
        }

        Friend f = new Friend();
        f.setUserId(fromId);
        f.setFriendId(toId);
        f.setStatus(ST_PENDING);
        f.setApplyMsg(applyText);
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
                .eq(Friend::getStatus, ST_PENDING)
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
            item.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
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
        if (f == null || !meId.equals(f.getFriendId()) || f.getStatus() == null || f.getStatus() != ST_PENDING) {
            return Result.fail("这条申请不存在或已经处理过了");
        }
        if (accept) {
            f.setStatus(ST_FRIEND);
            f.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(f);
            return Result.ok("已同意，你们是好友啦");
        }
        // 软删成「未通过」：申请人能在「我发出的申请」里看到结果，也能再次申请，
        // 同时不会像物理删除那样立刻被重复申请刷屏。
        f.setStatus(ST_REJECTED);
        f.setUpdateTime(LocalDateTime.now());
        friendMapper.updateById(f);
        return Result.ok("已拒绝");
    }

    @Override
    public Result<?> list(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, ST_FRIEND)
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
                .eq(Friend::getStatus, ST_FRIEND)
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, meId))
                .set(Friend::getStatus, ST_REMOVED)
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
        // 与前端 maxlength=16 保持一致
        if (text.length() > 16) {
            text = text.substring(0, 16);
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

    @Override
    public Result<?> sent(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, meId)
                .in(Friend::getStatus, Arrays.asList(ST_PENDING, ST_REJECTED))
                .orderByDesc(Friend::getUpdateTime)
                .last("limit 30"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Friend f : rows) {
            User u = userMapper.selectById(f.getFriendId());
            if (u == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", f.getId());
            item.put("userId", u.getId());
            item.put("name", u.getName());
            item.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
            item.put("applyMsg", f.getApplyMsg() == null ? "" : f.getApplyMsg());
            item.put("time", formatTime(f.getUpdateTime() == null ? f.getCreateTime() : f.getUpdateTime()));
            item.put("state", f.getStatus() == ST_PENDING ? "waiting" : "rejected");
            out.add(item);
        }
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancel(Long applyId, Long meId) {
        if (applyId == null || meId == null) {
            return Result.fail("参数不完整");
        }
        Friend f = friendMapper.selectById(applyId);
        if (f == null || !meId.equals(f.getUserId()) || f.getStatus() == null || f.getStatus() != ST_PENDING) {
            return Result.fail("这条申请不存在或已经处理过了");
        }
        // 撤回后彻底删掉，对方不会再看到，也可以立刻重新申请
        friendMapper.deleteById(applyId);
        return Result.ok("已撤回好友申请");
    }

    @Override
    public Result<?> profile(Long meId, Long friendId) {
        if (meId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, ST_FRIEND)
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, meId))
                .last("limit 1"));
        if (f == null) {
            return Result.fail("你们还不是好友");
        }
        User u = userMapper.selectById(friendId);
        if (u == null) {
            return Result.fail("这只小马不见了");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", u.getId());
        out.put("name", u.getName());
        out.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        out.put("points", u.getPoints() == null ? 0 : u.getPoints());
        out.put("joined", u.getCreateTime() == null ? "" : u.getCreateTime().toLocalDate().toString());
        out.put("since", formatTime(f.getCreateTime()));

        int days = 0;
        if (f.getCreateTime() != null) {
            days = (int) ChronoUnit.DAYS.between(f.getCreateTime().toLocalDate(), LocalDate.now());
        }
        out.put("days", days);

        String relationType = f.getRelationType() == null ? "" : f.getRelationType();
        out.put("relationType", relationType);

        FriendRemark r = friendRemarkMapper.selectOne(new LambdaQueryWrapper<FriendRemark>()
                .eq(FriendRemark::getMeId, meId)
                .eq(FriendRemark::getFriendId, friendId)
                .last("limit 1"));
        out.put("remark", r == null || r.getRemark() == null ? "" : r.getRemark());

        int msgCount = Math.toIntExact(chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromId, meId).eq(ChatMessage::getToId, friendId)
                        .or().eq(ChatMessage::getFromId, friendId).eq(ChatMessage::getToId, meId))));
        out.put("msgCount", msgCount);
        out.put("badges", ShopDefs.interactBadges(msgCount, days, relationType));

        // 共同好友：好友的好友，取交集
        Set<Long> mine = friendIds(meId);
        Set<Long> theirs = friendIds(friendId);
        List<String> mutualNames = new ArrayList<>();
        for (Long id : mine) {
            if (theirs.contains(id) && !id.equals(friendId)) {
                User mu = userMapper.selectById(id);
                if (mu != null) mutualNames.add(mu.getName());
            }
        }
        out.put("mutualCount", mutualNames.size());
        out.put("mutualNames", mutualNames.size() > 6 ? mutualNames.subList(0, 6) : mutualNames);

        // 游戏战绩：按 gameId 取各自最高分，再逐项比
        List<GameScore> myScores = gameScoreMapper.selectList(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getUserId, meId));
        List<GameScore> theirScores = gameScoreMapper.selectList(new LambdaQueryWrapper<GameScore>()
                .eq(GameScore::getUserId, friendId));
        Map<String, Integer> myBest = bestByGame(myScores);
        Map<String, Integer> theirBest = bestByGame(theirScores);
        int both = 0, win = 0, lose = 0, tie = 0;
        for (Map.Entry<String, Integer> e : myBest.entrySet()) {
            Integer other = theirBest.get(e.getKey());
            if (other == null) continue;
            both++;
            if (e.getValue() > other) win++;
            else if (e.getValue() < other) lose++;
            else tie++;
        }
        Map<String, Object> game = new LinkedHashMap<>();
        game.put("myGames", myBest.size());
        game.put("friendGames", theirBest.size());
        game.put("myPlays", myScores.size());
        game.put("friendPlays", theirScores.size());
        game.put("both", both);
        game.put("win", win);
        game.put("lose", lose);
        game.put("tie", tie);
        out.put("game", game);
        return Result.ok(out);
    }

    @Override
    public Result<?> suggest(Long meId) {
        if (meId == null) {
            return Result.fail("先登录再来查看吧");
        }
        Set<Long> myFriends = friendIds(meId);

        // 已经是好友 / 申请中的人不再推荐
        Set<Long> blocked = new HashSet<>();
        blocked.add(meId);
        List<Friend> relations = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .and(w -> w.eq(Friend::getUserId, meId).or().eq(Friend::getFriendId, meId)));
        for (Friend f : relations) {
            if (f.getStatus() != null && (f.getStatus() == ST_FRIEND || f.getStatus() == ST_PENDING)) {
                blocked.add(meId.equals(f.getUserId()) ? f.getFriendId() : f.getUserId());
            }
        }

        // 统计「好友的好友」出现次数
        Map<Long, Integer> mutual = new HashMap<>();
        Map<Long, String> viaName = new HashMap<>();
        for (Long fid : myFriends) {
            User viaUser = userMapper.selectById(fid);
            String via = viaUser == null ? "" : viaUser.getName();
            for (Long cand : friendIds(fid)) {
                if (blocked.contains(cand)) continue;
                mutual.merge(cand, 1, Integer::sum);
                viaName.putIfAbsent(cand, via);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        List<Map.Entry<Long, Integer>> sorted = mutual.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Long, Integer> e) -> e.getValue()).reversed())
                .collect(Collectors.toList());
        for (Map.Entry<Long, Integer> e : sorted) {
            if (out.size() >= 8) break;
            Map<String, Object> item = buildSuggestItem(e.getKey(), e.getValue(),
                    "有 " + e.getValue() + " 位共同好友 · 通过 " + viaName.getOrDefault(e.getKey(), "好友") + " 认识");
            if (item != null) out.add(item);
        }
        // 好友不多时，用「刚来糖霜镇的伙伴」把推荐区填满
        if (out.size() < 8) {
            List<User> rest = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .orderByDesc(User::getId).last("limit 20"));
            Set<Long> added = out.stream().map(m -> ((Number) m.get("id")).longValue()).collect(Collectors.toSet());
            for (User u : rest) {
                if (out.size() >= 8) break;
                if (blocked.contains(u.getId()) || added.contains(u.getId())) continue;
                Map<String, Object> item = buildSuggestItem(u.getId(), 0, "糖霜镇的新伙伴，打个招呼吧");
                if (item != null) {
                    out.add(item);
                    added.add(u.getId());
                }
            }
        }
        return Result.ok(out);
    }

    /** 组装一个推荐条目，账号不存在时返回 null */
    private Map<String, Object> buildSuggestItem(Long id, int mutualCount, String reason) {
        User u = userMapper.selectById(id);
        if (u == null) return null;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", u.getId());
        item.put("name", u.getName());
        item.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        item.put("mutual", mutualCount);
        item.put("reason", reason);
        item.put("relation", "none");
        return item;
    }

    /** 某人的好友 id 集合 */
    private Set<Long> friendIds(Long userId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, ST_FRIEND)
                .and(w -> w.eq(Friend::getUserId, userId).or().eq(Friend::getFriendId, userId)));
        Set<Long> ids = new LinkedHashSet<>();
        for (Friend f : rows) {
            ids.add(userId.equals(f.getUserId()) ? f.getFriendId() : f.getUserId());
        }
        return ids;
    }

    /** 按 gameId 归约出每款游戏的最高分 */
    private Map<String, Integer> bestByGame(List<GameScore> rows) {
        Map<String, Integer> best = new HashMap<>();
        for (GameScore g : rows) {
            if (g.getGameId() == null || g.getScore() == null) continue;
            best.merge(g.getGameId(), g.getScore(), Math::max);
        }
        return best;
    }

    private String normalizeApplyMsg(String msg) {
        String text = msg == null ? "" : msg.trim();
        if (text.isEmpty()) text = "say hi!";
        if (text.length() > 60) text = text.substring(0, 60);
        return text;
    }

    /** meId 与对方的关系：none / pending-out / pending-in / friend */
    private String relationOf(Long meId, Long otherId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .and(w -> w.eq(Friend::getUserId, meId).eq(Friend::getFriendId, otherId)
                        .or().eq(Friend::getUserId, otherId).eq(Friend::getFriendId, meId)));
        boolean hasFriend = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == ST_FRIEND);
        if (hasFriend) {
            return "friend";
        }
        boolean myApply = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == ST_PENDING
                && meId.equals(f.getUserId()));
        if (myApply) {
            return "pending-out";
        }
        boolean theirApply = rows.stream().anyMatch(f -> f.getStatus() != null && f.getStatus() == ST_PENDING
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
