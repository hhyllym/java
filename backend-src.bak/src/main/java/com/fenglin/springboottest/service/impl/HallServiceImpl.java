package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.HallMessage;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.HallMessageMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.HallService;
import com.fenglin.springboottest.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 星糖大厅实现。
 *
 * 设计取舍：
 *  - 聊天消息落库（t_hall_message）：大厅是公共空间，滚上去还能翻，刷新/重启都不丢。
 *  - 游戏邀请放内存：房间是「当下正在发生的事」，需要实时状态和 TTL，用内存最省事，
 *    和糖霜镇大富翁的房间同一套路（后端重启后邀请失效，重新发起即可）。
 *  - 不引 WebSocket：前端每 2~3 秒轮询一次，对「大厅聊天」这种延迟不敏感的场景足够，
 *    且不用加依赖、不用担心连接断开。
 */
@Service
public class HallServiceImpl implements HallService {

    /** 聊天记录最多带回来多少条 */
    private static final int MESSAGE_LIMIT = 100;
    /** 单条消息最长 */
    private static final int CONTENT_MAX = 500;
    /** 房间存活时长：2 小时没人管就自动清掉 */
    private static final long ROOM_TTL = 2L * 60 * 60 * 1000;
    /** 房间人数上下限 */
    private static final int MEMBER_MIN = 2;
    private static final int MEMBER_MAX = 4;

    /** 房间号字符集：去掉了容易看混的 I O 0 1 */
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /** 游戏 id -> 显示名（跟前端 src/courses.js 的 GAMES 保持一致） */
    private static final Map<String, String> GAME_NAMES = Map.ofEntries(
            Map.entry("monopoly", "糖霜镇大富翁"),
            Map.entry("wheel", "星辉幸运转盘"),
            Map.entry("card", "魔法翻牌·找星辉"),
            Map.entry("gacha", "彩虹扭蛋机"),
            Map.entry("jump", "小马跳跳跑"),
            Map.entry("mine", "星糖扫雷"),
            Map.entry("puzzle", "心印拼拼乐"),
            Map.entry("glide", "流光穿越"),
            Map.entry("race", "云驰冲刺赛"),
            Map.entry("snake", "彩虹贪吃蛇"),
            Map.entry("sports", "糖豆消消乐"),
            Map.entry("slot", "小马老虎机"),
            Map.entry("twilight", "魔法问答"),
            Map.entry("applejack", "苹果农庄"),
            Map.entry("dash", "闪电反应"),
            Map.entry("snooker", "台球对决"),
            Map.entry("match3", "三消糖果屋")
    );

    @Autowired
    private HallMessageMapper hallMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PresenceService presenceService;

    /** roomId -> 房间 */
    private final ConcurrentHashMap<String, HallRoom> rooms = new ConcurrentHashMap<>();

    // ======================= 聊天 =======================

    @Override
    public Result<?> messages(int limit) {
        int n = Math.max(1, Math.min(limit <= 0 ? MESSAGE_LIMIT : limit, MESSAGE_LIMIT));
        List<HallMessage> rows = hallMessageMapper.selectList(new LambdaQueryWrapper<HallMessage>()
                .orderByDesc(HallMessage::getId)
                .last("limit " + n));
        // 查出来是倒序（新的在前），翻正序返回，前端直接追加渲染
        Collections.reverse(rows);

        // 回填用户「当前」头像：消息表里存的是发消息那一刻的快照，
        // 用户后来换了头像，旧消息也应该显示新头像。
        Map<Long, String> currentAvatar = currentAvatars(rows);

        List<Map<String, Object>> out = new ArrayList<>();
        for (HallMessage m : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("name", m.getName());
            String av = m.getAvatar();
            if (m.getUserId() != null && currentAvatar.containsKey(m.getUserId())) {
                av = currentAvatar.get(m.getUserId());
            }
            item.put("avatar", av == null ? "" : av);
            item.put("content", m.getContent());
            item.put("type", m.getMsgType() == null ? "text" : m.getMsgType());
            item.put("time", formatTime(m.getCreateTime()));
            out.add(item);
        }
        return Result.ok(out);
    }

    /** 批量取这些消息作者当前的头像（userId -> avatar） */
    private Map<Long, String> currentAvatars(List<HallMessage> rows) {
        Map<Long, String> out = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>();
        for (HallMessage m : rows) {
            if (m.getUserId() != null && !ids.contains(m.getUserId())) ids.add(m.getUserId());
        }
        if (ids.isEmpty()) return out;
        for (User u : userMapper.selectBatchIds(ids)) {
            out.put(u.getId(), u.getAvatar() == null ? "" : u.getAvatar());
        }
        return out;
    }

    @Override
    public Result<?> send(Long userId, String content) {
        User user = requireUser(userId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            return Result.fail("说点什么再发呀～");
        }
        if (text.length() > CONTENT_MAX) {
            return Result.fail("消息太长啦，一次最多 " + CONTENT_MAX + " 个字");
        }

        HallMessage m = new HallMessage();
        m.setUserId(user.getId());
        m.setName(user.getName());
        m.setAvatar(user.getAvatar());
        m.setContent(text);
        m.setMsgType("text");
        m.setCreateTime(LocalDateTime.now());
        hallMessageMapper.insert(m);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", m.getId());
        item.put("userId", m.getUserId());
        item.put("name", m.getName());
        item.put("avatar", m.getAvatar());
        item.put("content", m.getContent());
        item.put("type", "text");
        item.put("time", formatTime(m.getCreateTime()));
        return Result.ok(item);
    }

    @Override
    public void systemNotice(String content) {
        if (content == null || content.trim().isEmpty()) return;
        HallMessage m = new HallMessage();
        m.setUserId(null);
        m.setName("星糖谷");
        m.setAvatar("🏠");
        m.setContent(content);
        m.setMsgType("system");
        m.setCreateTime(LocalDateTime.now());
        hallMessageMapper.insert(m);
    }

    // ======================= 在线用户 =======================

    @Override
       public Result<?> online() {
        List<Long> ids = presenceService.onlineUserIds();
        List<Map<String, Object>> out = new ArrayList<>();
        if (ids.isEmpty()) return Result.ok(out);

        List<User> users = userMapper.selectBatchIds(ids);
        for (User u : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("avatar", u.getAvatar());
            item.put("points", u.getPoints());
            out.add(item);
        }
        // 头像为空的排后面，列表好看一点
        out.sort(Comparator.comparing(a -> a.get("avatar") == null || "".equals(a.get("avatar")) ? 1 : 0));
        return Result.ok(out);
    }

    // ======================= 游戏邀请房间 =======================

    /** 房间里的一个人 */
    public static class Member {
        public Long userId;
        public String name;
        public String avatar;
        public long joined = System.currentTimeMillis();
    }

    /** 一个游戏邀请 */
    public static class HallRoom {
        public final String id;
        public String game;
        public String title;
        public Long hostId;
        public String hostName;
        public int maxMembers;
        public final List<Member> members = new ArrayList<>();
        public final long created = System.currentTimeMillis();
        public boolean closed = false;

        HallRoom(String id) {
            this.id = id;
        }
    }

    @Override
    public Result<?> rooms() {
        cleanExpired();
        // 房间成员的头像同样回填「当前」头像
        Map<Long, String> avs = currentAvatarsOfRooms();
        List<Map<String, Object>> out = new ArrayList<>();
        List<HallRoom> list = new ArrayList<>(rooms.values());
        // 新的在前
        list.sort(Comparator.comparingLong((HallRoom r) -> r.created).reversed());
        for (HallRoom r : list) {
            out.add(toMap(r, avs));
        }
        return Result.ok(out);
    }

    /** 收集所有房间里的人，取他们当前的头像 */
    private Map<Long, String> currentAvatarsOfRooms() {
        Map<Long, String> out = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>();
        for (HallRoom r : rooms.values()) {
            for (Member m : r.members) {
                if (m.userId != null && !ids.contains(m.userId)) ids.add(m.userId);
            }
        }
        if (ids.isEmpty()) return out;
        for (User u : userMapper.selectBatchIds(ids)) {
            out.put(u.getId(), u.getAvatar() == null ? "" : u.getAvatar());
        }
        return out;
    }

    @Override
    public Result<?> createRoom(Long userId, String game, String title, Integer maxMembers) {
        User user = requireUser(userId);
        String gameId = game == null ? "" : game.trim();
        if (gameId.isEmpty() || !GAME_NAMES.containsKey(gameId)) {
            return Result.fail("想邀请一起玩哪种游戏呀？");
        }
        int max = maxMembers == null ? MEMBER_MAX : Math.max(MEMBER_MIN, Math.min(maxMembers, MEMBER_MAX));
        String roomTitle = title == null ? "" : title.trim();
        if (roomTitle.isEmpty()) {
            roomTitle = GAME_NAMES.get(gameId) + " 一起玩";
        }
        if (roomTitle.length() > 30) {
            roomTitle = roomTitle.substring(0, 30);
        }

        // 一个人同时只能开一个邀请，免得刷屏。
        // 但如果手上这个邀请还没人加入（只有自己），就当成过期残留，直接顶掉重新开。
        for (HallRoom r : rooms.values()) {
            if (r.hostId.equals(user.getId()) && !r.closed) {
                if (r.members.size() <= 1) {
                    rooms.remove(r.id);
                    r.closed = true;
                    break;
                }
                return Result.fail("你已经有发起中的邀请啦，先把它关掉～");
            }
        }

        HallRoom room = new HallRoom(newRoomId());
        room.game = gameId;
        room.title = roomTitle;
        room.hostId = user.getId();
        room.hostName = user.getName();
        room.maxMembers = max;
        Member host = new Member();
        host.userId = user.getId();
        host.name = user.getName();
        host.avatar = user.getAvatar();
        room.members.add(host);
        rooms.put(room.id, room);

        systemNotice("🏠 " + user.getName() + " 发起了一个「" + roomTitle + "」邀请，等小马们来一起玩～");
        return Result.ok(toMap(room));
    }

    @Override
    public Result<?> joinRoom(Long userId, String roomId) {
        User user = requireUser(userId);
        HallRoom room = rooms.get(roomId == null ? "" : roomId.trim().toUpperCase());
        if (room == null || room.closed) {
            return Result.fail("这个邀请已经结束啦");
        }
        for (Member m : room.members) {
            if (m.userId.equals(user.getId())) {
                return Result.ok(toMap(room));
            }
        }
        if (room.members.size() >= room.maxMembers) {
            return Result.fail("这个邀请已经满员啦");
        }
        Member m = new Member();
        m.userId = user.getId();
        m.name = user.getName();
        m.avatar = user.getAvatar();
        room.members.add(m);
        systemNotice("🎉 " + user.getName() + " 加入了「" + room.title + "」");
        return Result.ok(toMap(room));
    }

    @Override
    public Result<?> leaveRoom(Long userId, String roomId) {
        User user = requireUser(userId);
        HallRoom room = rooms.get(roomId == null ? "" : roomId.trim().toUpperCase());
        if (room == null) {
            return Result.fail("这个邀请已经结束啦");
        }
        room.members.removeIf(m -> m.userId.equals(user.getId()));
        if (room.members.isEmpty() || room.hostId.equals(user.getId())) {
            // 房主走了或者没人了 → 房间直接关掉
            rooms.remove(room.id);
            room.closed = true;
        }
        return Result.ok(toMap(room));
    }

    @Override
    public Result<?> closeRoom(Long userId, String roomId) {
        User user = requireUser(userId);
        HallRoom room = rooms.get(roomId == null ? "" : roomId.trim().toUpperCase());
        if (room == null) {
            return Result.fail("这个邀请已经结束啦");
        }
        if (!room.hostId.equals(user.getId())) {
            return Result.fail("只有发起人能关闭这个邀请哦");
        }
        rooms.remove(room.id);
        room.closed = true;
        systemNotice("👋 " + user.getName() + " 关闭了「" + room.title + "」邀请");
        return Result.ok(toMap(room));
    }

    @Override
    public List<Map<String, Object>> roomsOf(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (userId == null) return out;
        for (HallRoom r : rooms.values()) {
            for (Member m : r.members) {
                if (m.userId.equals(userId)) {
                    out.add(toMap(r));
                    break;
                }
            }
        }
        return out;
    }

    // ======================= 内部工具 =======================

    private Map<String, Object> toMap(HallRoom r) {
        return toMap(r, null);
    }

    /**
     * @param avs userId -> 当前头像；为 null 时用成员进房时的快照
     */
    private Map<String, Object> toMap(HallRoom r, Map<Long, String> avs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("game", r.game);
        m.put("gameName", GAME_NAMES.getOrDefault(r.game, r.game));
        m.put("title", r.title);
        m.put("hostId", r.hostId);
        m.put("hostName", r.hostName);
        m.put("maxMembers", r.maxMembers);
        m.put("count", r.members.size());
        m.put("created", r.created);
        List<Map<String, Object>> ms = new ArrayList<>();
        for (Member mem : r.members) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("userId", mem.userId);
            one.put("name", mem.name);
            String av = mem.avatar;
            if (avs != null && mem.userId != null && avs.containsKey(mem.userId)) {
                av = avs.get(mem.userId);
            }
            one.put("avatar", av == null ? "" : av);
            one.put("host", r.hostId.equals(mem.userId));
            ms.add(one);
        }
        m.put("members", ms);
        return m;
    }

    /** 生成不重复的 4 位房间号 */
    private String newRoomId() {
        for (int i = 0; i < 50; i++) {
            StringBuilder sb = new StringBuilder(4);
            for (int j = 0; j < 4; j++) {
                sb.append(CODE_CHARS[(int) (Math.random() * CODE_CHARS.length)]);
            }
            String id = sb.toString();
            if (!rooms.containsKey(id)) return id;
        }
        // 极端情况下退化成 UUID 前 4 位
        return UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /** 清掉超过 TTL 没人管的房间 */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        rooms.values().removeIf(r -> now - r.created > ROOM_TTL);
    }

    /** 取用户；取不到就说明没登录 / 账号不存在 */
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new HallException("先登录再来大厅玩吧～");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new HallException("这个账号找不到了，重新登录试试？");
        }
        return u;
    }

    private String formatTime(LocalDateTime t) {
        if (t == null) return "";
        return t.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }

    /** 业务异常：Controller 转成 { code:400, msg } */
    public static class HallException extends RuntimeException {
        public HallException(String msg) {
            super(msg);
        }
    }
}
