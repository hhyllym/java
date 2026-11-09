package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🎲 糖霜镇大富翁「四人联机房间」：内存实现，零新依赖、零建表。
 *
 * 设计要点：
 *  - 服务器是唯一裁判：棋盘、骰子、买店收租全在后端算，前端只渲染状态 + 发动作，
 *    四个浏览器看到的结果永远一致，也不会因为某端刷新就不同步。
 *  - 前端每 ~1.2 秒轮询 /state，回合制对延迟不敏感，不需要 WebSocket。
 *  - 房间号 4 位短码，座位用 token 校验身份；刷新页面重进同名房间会回到原座位。
 *  - 空位可由电脑补位；轮到的人 20 秒没动作，服务器自动代走，一人掉线不卡死整局。
 */
@Service
public class MonopolyRoomService {

    private static final int SEAT_COUNT = 4;
    private static final int MAX_ROUND = 8;
    private static final long ROOM_TTL = 2L * 60 * 60 * 1000;
    private static final long ONLINE_MS = 15_000L;
    private static final long AUTO_PLAY_MS = 20_000L;

    /** 上桌门槛：4 人局最后一名的最大亏损，开局前要求每位真人至少有这么多可用积分 */
    public static final int MIN_STAKE = 45;

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String[] PONY_NAME = { "星澜", "云驰", "泡芙", "金穗" };
    private static final String[] PONY_ICON = { "🦄", "🌈", "🎈", "🍎" };
    private static final String[] PONY_COLOR = { "#a06cd5", "#6ec6e6", "#f39ac0", "#eda14e" };
    private static final int[] PONY_TEAM = { 1, 2, 1, 2 };

    /** 格子定义：type / 图标 / 名字 / 售价 / 租金 / 奖励分 */
    private record Tile(String type, String icon, String name, int price, int rent, int value) {}
    private static final List<Tile> TILES = List.of(
            new Tile("start",  "🏰", "星辉城堡",   0,  0,  0),
            new Tile("shop",   "🍎", "金穗果园",  20, 10,  0),
            new Tile("event",  "✨", "魔法事件",   0,  0,  0),
            new Tile("bonus",  "🎈", "派对广场",   0,  0, 18),
            new Tile("shop",   "🧁", "甜品小屋",  24, 12,  0),
            new Tile("chance", "🎁", "机会卡",     0,  0,  0),
            new Tile("bonus",  "🌈", "彩虹瀑布",   0,  0, 25),
            new Tile("shop",   "💎", "珍品时装",  32, 16,  0),
            new Tile("bonus",  "⚡", "闪电赛道",   0,  0, 20),
            new Tile("shop",   "🍬", "糖果小铺",  18,  9,  0),
            new Tile("event",  "✨", "魔法事件",   0,  0,  0),
            new Tile("shop",   "🏰", "云顶城",  40, 20,  0),
            new Tile("bonus",  "🎪", "超级派对",   0,  0, 30),
            new Tile("chance", "🎁", "机会卡",     0,  0,  0),
            new Tile("shop",   "🦋", "动物之家",  22, 11,  0),
            new Tile("tax",    "💸", "税务所",     0,  0, 14),
            new Tile("shop",   "📚", "金橡树书屋", 28, 14,  0),
            new Tile("event",  "✨", "魔法事件",   0,  0,  0),
            new Tile("shop",   "🥤", "苹果汁摊",  16,  8,  0),
            new Tile("chance", "🎁", "机会卡",     0,  0,  0),
            new Tile("bonus",  "🌸", "茸茸花园",   0,  0, 22),
            new Tile("shop",   "🎀", "礼品店",    26, 13,  0),
            new Tile("event",  "🌩️", "疾风云",     0,  0,  0),
            new Tile("bonus",  "🎠", "彩带旋转台",   0,  0, 15)
    );

    /** 机会 / 事件卡池，四位玩家共用 */
    private record Card(String text, int score, int coins) {}
    private static final List<Card> CARDS = List.of(
            new Card("🎉 泡芙请你吃蛋糕，+25 分", 25, 0),
            new Card("🍎 帮金穗收苹果，+20 宝石币", 0, 20),
            new Card("📚 星澜送你一本魔法书，+30 分", 30, 0),
            new Card("💎 绮罗送了件新礼服，+15 分", 15, 0),
            new Card("🌈 一阵疾风吹跑了 10 宝石币", 0, -10),
            new Card("🦋 陪茸茸找小动物，-8 分", -8, 0),
            new Card("🧁 甜品屋大促销，+15 宝石币", 0, 15),
            new Card("🎈 派对抽奖中奖啦，+20 分", 20, 0)
    );

    /** 业务异常：Controller 会转成 { code:400, msg } */
    public static class RoomException extends RuntimeException {
        public RoomException(String msg) { super(msg); }
    }

    public static class Seat {
        public int id;
        public String token;
        public String name;
        public Long userId;
        public boolean ai;
        public long lastSeen = System.currentTimeMillis();
        public int pos = 0;
        public int coins = 80;
        public int score = 0;
        public final List<Integer> shops = new ArrayList<>();
        /** 本局真实积分收支：正数进账、负数扣除，终局由服务器写回账号 */
        public int pointDelta = 0;
    }

    public static class Room {
        public final String id;
        public final long created = System.currentTimeMillis();
        public int version = 1;
        public String status = "waiting";   // waiting | playing | buy | result
        public String mode = "team";        // team | solo
        public int round = 1;
        public int turn = 0;
        public int dice = 0;
        public String log = "房间已就绪，等四位小马到齐～";
        public final Seat[] seats = new Seat[SEAT_COUNT];
        public final int[] owner;           // -1 无主，否则是座位号
        public int buyTile = -1;
        public int buySeat = -1;
        public long turnAt = 0;
        public int winnerTeam = 0;
        public List<Map<String, Object>> ranking = new ArrayList<>();

        public Room(String id) {
            this.id = id;
            this.owner = new int[TILES.size()];
            Arrays.fill(this.owner, -1);
        }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Autowired
    private UserController userController;

    /* ==================== 对外接口 ==================== */

    public synchronized Map<String, Object> create(String nickname, Long userId, String mode) {
        sweep();
        Room r = new Room(newCode());
        if ("solo".equals(mode)) r.mode = "solo";
        Seat host = humanSeat(0, nickname, userId);
        r.seats[0] = host;
        r.log = host.name + " 开了房间，快把房间号发给小伙伴吧！";
        rooms.put(r.id, r);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("roomId", r.id);
        out.put("seatId", 0);
        out.put("token", host.token);
        out.put("state", snapshot(r, 0));
        return out;
    }

    public synchronized Map<String, Object> join(String roomId, String nickname, Long userId) {
        Room r = need(roomId);
        if (!"waiting".equals(r.status)) {
            throw new RoomException("这局已经开始了，等下一局再进吧");
        }
        String name = cleanName(nickname, 0);
        // 同名真人重进（刷新页面 / 手滑点了两次）直接回到原座位，不占新坑
        for (int i = 0; i < SEAT_COUNT; i++) {
            Seat old = r.seats[i];
            if (old != null && !old.ai && name.equals(old.name)) {
                old.lastSeen = System.currentTimeMillis();
                old.userId = userId;
                return seatResult(r, i);
            }
        }
        int idx = -1;
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (r.seats[i] == null) { idx = i; break; }
        }
        if (idx < 0) throw new RoomException("房间已经满 4 人啦");

        r.seats[idx] = humanSeat(idx, name, userId);
        r.version++;
        r.log = name + " 加入了房间（" + humanCount(r) + "/4）";
        return seatResult(r, idx);
    }

    public synchronized Map<String, Object> start(String roomId, int seatId, String token,
                                                   String mode, boolean fillAi) {
        Room r = need(roomId);
        auth(r, seatId, token);
        if (seatId != 0) throw new RoomException("只有房主能开始游戏");
        if (!"waiting".equals(r.status)) throw new RoomException("游戏已经开始啦");

        int humans = humanCount(r);
        if (humans < SEAT_COUNT && !fillAi) {
            throw new RoomException("还差 " + (SEAT_COUNT - humans) + " 人，四个人到齐再开始（也可以点「电脑补位」先玩）");
        }
        if ("solo".equals(mode)) r.mode = "solo";
        else if ("team".equals(mode)) r.mode = "team";

        // 上桌前确认每位真人的可用积分够当本金，不够就别开，免得终局扣成负数
        for (int i = 0; i < SEAT_COUNT; i++) {
            Seat chk = r.seats[i];
            if (chk != null && !chk.ai && chk.userId != null
                    && userController.pointsOf(chk.userId) < MIN_STAKE) {
                throw new RoomException("「" + chk.name + "」的可用积分不足 " + MIN_STAKE
                        + "，先去「💰 赚积分」页攒一点再开局吧");
            }
        }
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (r.seats[i] == null) r.seats[i] = aiSeat(i);
            Seat s = r.seats[i];
            s.pos = 0;
            s.coins = 80;
            s.score = 0;
            s.pointDelta = 0;
            s.shops.clear();
            s.lastSeen = System.currentTimeMillis();
        }
        Arrays.fill(r.owner, -1);
        r.buyTile = -1;
        r.buySeat = -1;
        r.ranking = new ArrayList<>();
        r.winnerTeam = 0;
        r.round = 1;
        r.turn = 0;
        r.dice = 0;
        r.status = "playing";
        r.turnAt = System.currentTimeMillis();
        r.log = "第 1 / " + MAX_ROUND + " 轮开始，轮到 " + iconOf(r, 0) + " " + nameOf(r, 0) + " 掷骰子！";
        r.version++;
        pump(r);
        return snapshot(r, seatId);
    }

    public synchronized Map<String, Object> state(String roomId, int seatId, String token) {
        Room r = need(roomId);
        Seat me = auth(r, seatId, token);
        me.lastSeen = System.currentTimeMillis();
        tick(r);
        return snapshot(r, seatId);
    }

    public synchronized Map<String, Object> action(String roomId, int seatId, String token,
                                                    String type, boolean value) {
        Room r = need(roomId);
        Seat me = auth(r, seatId, token);
        me.lastSeen = System.currentTimeMillis();
        tick(r);

        if ("roll".equals(type)) {
            if (!"playing".equals(r.status)) throw new RoomException("现在不能掷骰子");
            if (r.turn != seatId) throw new RoomException("还没轮到你哦，等 " + nameOf(r, r.turn) + " 走完");
            if (me.ai) throw new RoomException("这个座位由电脑托管");
            doTurn(r, seatId, false);
            pump(r);
        } else if ("buy".equals(type)) {
            if (!"buy".equals(r.status) || r.buySeat != seatId) throw new RoomException("现在不是你做买店决定");
            resolveBuy(r, seatId, value);
            pump(r);
        } else if ("again".equals(type)) {
            if (seatId != 0) throw new RoomException("只有房主能开下一局");
            if (!"result".equals(r.status)) throw new RoomException("这一局还没结束呢");
            // 把「掉线被电脑接管」的座位释放出来，方便同学重新加入
            for (int i = 0; i < SEAT_COUNT; i++) {
                Seat s = r.seats[i];
                if (s == null) continue;
                if (s.ai) r.seats[i] = null;
                else s.name = s.name.replace("（电脑接管）", "");
            }
            r.status = "waiting";
            r.buyTile = -1;
            r.buySeat = -1;
            r.log = "上一局结束啦，房主点「开始」就能再来一局～";
            r.version++;
        } else {
            throw new RoomException("未知操作：" + type);
        }
        return snapshot(r, seatId);
    }

    public synchronized void leave(String roomId, int seatId, String token) {
        Room r = rooms.get(normalize(roomId));
        if (r == null) return;
        Seat s = auth(r, seatId, token);
        if ("waiting".equals(r.status)) {
            r.seats[seatId] = null;
            r.log = s.name + " 离开了房间（" + humanCount(r) + "/4）";
        } else {
            // 对局中离开：交给电脑接着打，不拖累另外三个人
            s.ai = true;
            s.name = s.name + "（电脑接管）";
            r.log = s.name + " 掉线了，电脑替他继续走～";
        }
        r.version++;
    }

    /* ==================== 回合逻辑 ==================== */

    /** 轮到 AI 座位时自动走，最多连走 4 次（防死循环） */
    private void pump(Room r) {
        int guard = 0;
        while ("playing".equals(r.status) && guard++ < SEAT_COUNT
                && r.seats[r.turn] != null && r.seats[r.turn].ai) {
            doTurn(r, r.turn, true);
        }
    }

    /** 掉线兜底：真人 20 秒不动，系统替他走完这一步 */
    private void tick(Room r) {
        long now = System.currentTimeMillis();
        if ("buy".equals(r.status)) {
            if (now - r.turnAt > AUTO_PLAY_MS) {
                resolveBuy(r, r.buySeat, false);
                pump(r);
            }
        } else if ("playing".equals(r.status) && now - r.turnAt > AUTO_PLAY_MS) {
            doTurn(r, r.turn, true);
            pump(r);
        }
    }

    private void doTurn(Room r, int seatId, boolean auto) {
        Seat s = r.seats[seatId];
        if (s == null) { advanceTurn(r); return; }

        int d = ThreadLocalRandom.current().nextInt(1, 7);
        r.dice = d;
        String who = PONY_ICON[seatId] + " " + s.name;
        r.log = who + " 掷出 " + d + " 点";

        for (int k = 0; k < d; k++) {
            int next = (s.pos + 1) % TILES.size();
            if (next == 0) {
                s.score += 20;
                s.coins += 10;
                r.log += "，经过城堡 +20 分 / +10 币";
            }
            s.pos = next;
        }

        boolean pendingBuy = resolveLand(r, s, seatId, auto);
        r.version++;
        if (pendingBuy) return;
        advanceTurn(r);
    }

    /** 落地结算，返回 true 表示进入「等玩家决定买不买」 */
    private boolean resolveLand(Room r, Seat s, int seatId, boolean auto) {
        int idx = s.pos;
        Tile t = TILES.get(idx);
        String who = PONY_ICON[seatId] + " " + s.name;

        if ("shop".equals(t.type())) {
            int owner = r.owner[idx];
            if (owner < 0) {
                if (s.coins < t.price()) {
                    r.log = who + " 想买" + t.name() + "，可惜宝石币不够";
                    return false;
                }
                boolean yes;
                if (auto) {
                    yes = s.coins - t.price() >= 12 && ThreadLocalRandom.current().nextDouble() < 0.78;
                } else {
                    r.status = "buy";
                    r.buyTile = idx;
                    r.buySeat = seatId;
                    r.log = "🏠 " + who + " 来到" + t.name() + "，花 " + t.price() + " 币买下吗？";
                    return true;
                }
                if (yes) {
                    buyShop(r, s, seatId, idx, t.price());
                    r.log = "🏠 " + who + " 买下" + t.name() + "，租金 " + t.rent() + " 分";
                } else {
                    r.log = who + " 路过" + t.name() + "，没买";
                }
            } else if (owner == seatId) {
                int income = Math.round(t.rent() / 2f);
                s.coins += income;
                r.log = "🏠 " + who + " 回到自己的" + t.name() + "，营收 +" + income + " 币";
            } else {
                Seat lord = r.seats[owner];
                boolean teammate = "team".equals(r.mode) && PONY_TEAM[owner] == PONY_TEAM[seatId];
                if (teammate) {
                    int gift = Math.round(t.rent() / 2f);
                    s.coins += gift;
                    r.log = "🤝 " + who + " 踩到队友 " + (lord == null ? "小马" : lord.name)
                            + " 的" + t.name() + "，免租还收到 " + gift + " 币";
                } else {
                    int pay = Math.min(t.rent(), s.coins);
                    s.coins -= pay;
                    if (lord != null) {
                        lord.score += pay;
                        lord.coins += pay;
                    }
                    r.log = "💰 " + who + " 踩到 " + (lord == null ? "对手" : lord.name)
                            + " 的" + t.name() + "，付租 " + pay + " 币（对方 +" + pay + " 分）";
                }
            }
        } else if ("bonus".equals(t.type())) {
            s.score += t.value();
            r.log = t.icon() + " " + t.name() + " 打卡成功，" + who + " +" + t.value() + " 分！";
        } else if ("tax".equals(t.type())) {
            int pay = Math.min(t.value(), s.coins);
            s.coins -= pay;
            r.log = "💸 " + who + " 在税务所交了 " + pay + " 币";
        } else if ("start".equals(t.type())) {
            s.coins += 10;
            r.log = "🏰 " + who + " 停在星辉城堡，补给 +10 币";
        } else {
            Card c = CARDS.get(ThreadLocalRandom.current().nextInt(CARDS.size()));
            s.score = Math.max(0, s.score + c.score());
            s.coins = Math.max(0, s.coins + c.coins());
            r.log = who + " 抽卡：" + c.text();
        }
        return false;
    }

    private void resolveBuy(Room r, int seatId, boolean yes) {
        Seat s = r.seats[seatId];
        int idx = r.buyTile;
        if (s != null && idx >= 0) {
            Tile t = TILES.get(idx);
            if (yes && s.coins >= t.price() && r.owner[idx] < 0) {
                buyShop(r, s, seatId, idx, t.price());
                r.log = "🏠 " + PONY_ICON[seatId] + " " + s.name + " 买下" + t.name() + "！";
            } else {
                r.log = PONY_ICON[seatId] + " " + s.name + " 没买" + (idx >= 0 ? TILES.get(idx).name() : "店铺") + "～";
            }
        }
        r.status = "playing";
        r.buyTile = -1;
        r.buySeat = -1;
        r.version++;
        advanceTurn(r);
    }

    private void buyShop(Room r, Seat s, int seatId, int idx, int price) {
        s.coins -= price;
        s.shops.add(idx);
        r.owner[idx] = seatId;
    }

    private void advanceTurn(Room r) {
        if (r.turn >= SEAT_COUNT - 1) {
            if (r.round >= MAX_ROUND) { finish(r); return; }
            r.round++;
            r.turn = 0;
        } else {
            r.turn++;
        }
        r.turnAt = System.currentTimeMillis();
        r.log += " · 轮到 " + PONY_ICON[r.turn] + " " + nameOf(r, r.turn);
        r.version++;
    }

    private void finish(Room r) {
        r.status = "result";
        List<Seat> sorted = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) if (r.seats[i] != null) sorted.add(r.seats[i]);
        sorted.sort(Comparator.comparingInt((Seat s) -> scoreOf(s)).reversed());
        List<Map<String, Object>> rank = new ArrayList<>();
        for (Seat s : sorted) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.id);
            row.put("name", s.name);
            row.put("icon", PONY_ICON[s.id]);
            row.put("color", PONY_COLOR[s.id]);
            row.put("team", PONY_TEAM[s.id]);
            row.put("score", scoreOf(s));
            row.put("coins", s.coins);
            row.put("shops", s.shops.size());
            rank.add(row);
        }
        r.ranking = rank;
        int a = teamScore(r, 1), b = teamScore(r, 2);
        r.winnerTeam = "team".equals(r.mode) ? (a == b ? 0 : (a > b ? 1 : 2)) : 0;
        r.log = "🏁 巡游结束！" + ("team".equals(r.mode)
                ? (r.winnerTeam == 0 ? "两队平局" : (r.winnerTeam == 1 ? "🌟 星光队获胜" : "🌈 彩虹队获胜"))
                : "看看谁分最高");
        settlePoints(r);
        r.version++;
    }

    /**
     * 终局把「可用积分」按真人名次零和结算，直接写回账号余额并记流水。
     * 只有 2 位以上真人才结算（单人局不给分，防止自己开房刷积分），
     * AI 座位不参与；分配表合计恒为 0，不会凭空多出积分。
     */
    private void settlePoints(Room r) {
        List<Seat> humans = new ArrayList<>();
        for (Map<String, Object> row : r.ranking) {
            int id = ((Number) row.get("id")).intValue();
            Seat s = r.seats[id];
            if (s != null && !s.ai && s.userId != null) humans.add(s);
        }
        int n = humans.size();
        if (n < 2) return;
        int[] table;
        if (n == 2) table = new int[]{25, -25};
        else if (n == 3) table = new int[]{30, 0, -30};
        else table = new int[]{45, 15, -15, -45};

        for (int i = 0; i < n; i++) {
            Seat s = humans.get(i);
            int delta = table[i];
            // 余额兜底：中途去商店花掉了也不会扣成负数
            int balance = userController.pointsOf(s.userId);
            int actual = delta < 0 ? -Math.min(-delta, balance) : delta;
            if (actual != 0) {
                userController.addPoints(s.userId, actual, "monopoly",
                        "大富翁第 " + (i + 1) + " 名：" + (actual > 0 ? "+" : "") + actual + " 积分");
            }
            s.pointDelta = actual;
        }
        for (Map<String, Object> row : r.ranking) {
            int id = ((Number) row.get("id")).intValue();
            Seat s = r.seats[id];
            row.put("pointDelta", s == null ? 0 : s.pointDelta);
        }
    }

    /* ==================== 状态快照 ==================== */

    private Map<String, Object> snapshot(Room r, int mySeat) {
        long now = System.currentTimeMillis();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roomId", r.id);
        m.put("version", r.version);
        m.put("status", r.status);
        m.put("mode", r.mode);
        m.put("round", r.round);
        m.put("maxRound", MAX_ROUND);
        m.put("turn", r.turn);
        m.put("dice", r.dice);
        m.put("log", r.log);
        m.put("mySeat", mySeat);
        m.put("myScore", myScore(r, mySeat));
        m.put("myPointDelta", mySeat >= 0 && mySeat < SEAT_COUNT && r.seats[mySeat] != null
                ? r.seats[mySeat].pointDelta : 0);
        m.put("minStake", MIN_STAKE);
        m.put("myTeam", PONY_TEAM[mySeat]);
        m.put("hostSeat", 0);
        m.put("humanCount", humanCount(r));
        m.put("winnerTeam", r.winnerTeam);
        m.put("turnAt", r.turnAt);
        m.put("serverTime", now);

        List<Map<String, Object>> players = new ArrayList<>();
        for (int i = 0; i < SEAT_COUNT; i++) {
            Seat s = r.seats[i];
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", i);
            p.put("name", s == null ? "等待加入" : s.name);
            p.put("icon", PONY_ICON[i]);
            p.put("color", PONY_COLOR[i]);
            p.put("team", PONY_TEAM[i]);
            p.put("empty", s == null);
            p.put("ai", s != null && s.ai);
            p.put("online", s != null && !s.ai && now - s.lastSeen < ONLINE_MS);
            p.put("isMe", i == mySeat);
            p.put("pos", s == null ? 0 : s.pos);
            p.put("coins", s == null ? 0 : s.coins);
            p.put("score", s == null ? 0 : scoreOf(s));
            p.put("pointDelta", s == null ? 0 : s.pointDelta);
            p.put("shops", s == null ? new ArrayList<>() : new ArrayList<>(s.shops));
            players.add(p);
        }
        m.put("players", players);

        List<Map<String, Object>> tiles = new ArrayList<>();
        for (int i = 0; i < TILES.size(); i++) {
            Tile t = TILES.get(i);
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("i", i);
            o.put("type", t.type());
            o.put("icon", t.icon());
            o.put("name", t.name());
            o.put("price", t.price());
            o.put("rent", t.rent());
            o.put("value", t.value());
            o.put("owner", r.owner[i]);
            tiles.add(o);
        }
        m.put("tiles", tiles);

        if (r.buyTile >= 0) {
            Tile t = TILES.get(r.buyTile);
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("i", r.buyTile);
            b.put("name", t.name());
            b.put("price", t.price());
            m.put("buyTile", b);
        } else {
            m.put("buyTile", null);
        }
        m.put("buySeat", r.buySeat);
        m.put("ranking", r.ranking);
        m.put("teamScore1", teamScore(r, 1));
        m.put("teamScore2", teamScore(r, 2));
        return m;
    }

    /* ==================== 小工具 ==================== */

    private int scoreOf(Seat s) {
        return Math.max(0, s.score + s.shops.size() * 12);
    }

    private int teamScore(Room r, int team) {
        int sum = 0;
        for (int i = 0; i < SEAT_COUNT; i++) {
            if (PONY_TEAM[i] == team && r.seats[i] != null) sum += scoreOf(r.seats[i]);
        }
        return sum;
    }

    /** 计入成绩的分：组队按队伍合计，混战按个人 */
    private int myScore(Room r, int seatId) {
        if (seatId < 0 || seatId >= SEAT_COUNT) return 0;
        if ("team".equals(r.mode)) return teamScore(r, PONY_TEAM[seatId]);
        Seat s = r.seats[seatId];
        return s == null ? 0 : scoreOf(s);
    }

    private int humanCount(Room r) {
        int n = 0;
        for (Seat s : r.seats) if (s != null && !s.ai) n++;
        return n;
    }

    private String nameOf(Room r, int i) {
        Seat s = r.seats[i];
        return s == null ? PONY_NAME[i] : s.name;
    }

    private String iconOf(Room r, int i) { return PONY_ICON[i]; }

    private Seat humanSeat(int id, String nickname, Long userId) {
        Seat s = new Seat();
        s.id = id;
        s.token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        s.name = cleanName(nickname, id);
        s.userId = userId;
        s.ai = false;
        return s;
    }

    private Seat aiSeat(int id) {
        Seat s = new Seat();
        s.id = id;
        s.token = "ai-" + id;
        s.name = PONY_NAME[id] + "（电脑）";
        s.ai = true;
        return s;
    }

    private String cleanName(String raw, int id) {
        String s = raw == null ? "" : raw.trim().replaceAll("[\\r\\n]", "");
        if (s.isEmpty()) s = PONY_NAME[id];
        return s.length() > 12 ? s.substring(0, 12) : s;
    }

    private Map<String, Object> seatResult(Room r, int seatId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("roomId", r.id);
        out.put("seatId", seatId);
        out.put("token", r.seats[seatId].token);
        out.put("state", snapshot(r, seatId));
        return out;
    }

    private Room need(String roomId) {
        Room r = rooms.get(normalize(roomId));
        if (r == null) throw new RoomException("房间不存在或已过期，检查一下房间号");
        return r;
    }

    private Seat auth(Room r, int seatId, String token) {
        if (seatId < 0 || seatId >= SEAT_COUNT || r.seats[seatId] == null) {
            throw new RoomException("座位不存在");
        }
        Seat s = r.seats[seatId];
        if (token == null || !token.equals(s.token)) {
            throw new RoomException("身份校验失败，请重新加入房间");
        }
        return s;
    }

    private String normalize(String roomId) {
        return roomId == null ? "" : roomId.trim().toUpperCase();
    }

    private String newCode() {
        String code;
        int guard = 0;
        do {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_CHARS[ThreadLocalRandom.current().nextInt(CODE_CHARS.length)]);
            }
            code = sb.toString();
        } while (rooms.containsKey(code) && guard++ < 50);
        return code;
    }

    /** 顺手清掉两小时前的旧房间 */
    private void sweep() {
        long now = System.currentTimeMillis();
        rooms.entrySet().removeIf(e -> now - e.getValue().created > ROOM_TTL);
    }
}
