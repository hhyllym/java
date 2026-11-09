package com.fenglin.springboottest.common;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分商店 / 每日任务 / 宠物的静态定义。
 * 商品与任务都在这里登记，Controller 只查表，不写死逻辑。
 */
public final class ShopDefs {

    private ShopDefs() {
    }

    /**
     * 每日任务：id / 图标 / 名称 / 描述 / 积分 / 上报类型 / 目标次数 / 去哪完成。
     * type 是前端上报进度的动作名；bonus（全勤宝箱）不允许外部上报，
     * 只能由 TaskController 在其它任务全部领完后自动解锁。
     */
    public static List<Map<String, Object>> dailyTasks() {
        return Arrays.asList(
                task("daily_login",   "🌅", "每日登录",     "打开小马乐园就算打卡",        5,  "login",   1, "/dashboard"),
                task("daily_msg",     "💬", "消息小能手",   "给好友发送 3 条消息",        15, "msg",     3, "/friends"),
                task("daily_game",    "🎮", "游戏时间",     "玩 1 局小游戏",              15, "game",    1, "/games"),
                task("daily_view",    "📚", "图鉴学者",     "浏览 3 次小马图鉴",          10, "view",    3, "/home"),
                task("daily_checkin", "✅", "学院打卡",     "去学院完成每日打卡",         10, "checkin", 1, "/learn"),
                task("daily_lesson",  "📖", "认真听课",     "学完 1 节课程小节",          15, "lesson",  1, "/learn"),
                task("daily_quiz",    "✍️", "小测高手",     "答对 2 次课后小测",          15, "quiz",    2, "/learn"),
                task("daily_friend",  "🤝", "结交新朋友",   "成功加 1 位好友",            20, "friend",  1, "/friends"),
                task("daily_pet",     "🐾", "照顾小伙伴",   "喂一喂或陪宠物玩 2 次",       10, "pet",     2, "/pet"),
                task("daily_room",    "🎲", "联机大富翁",   "打完 1 局四人联机大富翁",     25, "room",    1, "/game/monopoly"),
                task("daily_gift",    "🧧", "友谊红包",     "送好友 1 次积分红包",         15, "gift",    1, "/friends"),
                task("daily_shop",    "🛍️", "逛街达人",     "在积分商店兑换 1 件商品",      5,  "shop",    1, "/shop"),
                task("daily_all",     "👑", "全勤宝箱",     "领完今天其它 12 个奖励自动解锁", 40, "bonus", 1, "/earn")
        );
    }

    private static Map<String, Object> task(String id, String icon, String name, String desc,
                                            int points, String type, int target, String go) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("icon", icon);
        m.put("name", name);
        m.put("desc", desc);
        m.put("points", points);
        m.put("type", type);
        m.put("target", target);
        m.put("go", go);
        return m;
    }

    /** 商店商品 */
    public static List<Map<String, Object>> shopItems() {
        return Arrays.asList(
                item("pet-egg-cat", "狸花猫蛋", "🐱", 200, "pet", "中华田园猫，机灵又黏人", "cat"),
                item("pet-egg-dog", "萨摩耶蛋", "🐶", 200, "pet", "微笑天使，蓬松得像一团雪", "dog"),
                item("pet-egg-bird", "玄凤鹦鹉蛋", "🦜", 200, "pet", "圆圆腮红加头顶一撮呆毛", "bird"),
                item("pet-egg-rabbit", "垂耳兔蛋", "🐰", 200, "pet", "耳朵软软垂下来，胆子小小", "rabbit"),
                item("pet-egg-fox", "赤狐蛋", "🦊", 200, "pet", "大尾巴扫来扫去，鬼点子最多", "fox"),
                item("pet-egg-bear", "棕熊蛋", "🧸", 200, "pet", "圆滚滚慢半拍，最喜欢抱抱", "bear"),
                item("pet-egg-hamster", "金丝熊蛋", "🐹", 200, "pet", "腮帮塞得鼓鼓的，捧着一颗瓜子", "hamster"),
                item("pet-egg-penguin", "小企鹅蛋", "🐧", 200, "pet", "走路一摇一摆，怕热不怕冷", "penguin"),
                item("pet-egg-mouse", "花枝鼠蛋", "🐭", 200, "pet", "大耳朵细尾巴，聪明得很", "mouse"),
                item("pet-egg-squirrel", "红松鼠蛋", "🐿️", 200, "pet", "蓬松大尾巴，松果永远抱在怀里", "squirrel"),
                item("pet-egg-ragdoll", "布偶猫蛋", "🐈", 200, "pet", "蓝眼睛软乎乎，抱住就不撒手", "ragdoll"),
                item("pet-egg-corgi", "柯基犬蛋", "🐕", 200, "pet", "小短腿大耳朵，走路屁股扭扭", "corgi"),
                item("pet-egg-shiba", "柴犬蛋", "🦮", 200, "pet", "圆脸微笑，有点小傲娇", "shiba"),
                item("pet-egg-budgie", "虎皮鹦鹉蛋", "🦜", 200, "pet", "蓝黄配色，叽叽喳喳学你说话", "budgie"),
                item("pet-food", "宠物口粮", "🥣", 60, "food", "喂宠物用，饱食度 +25", ""),
                item("pet-cake", "宠物蛋糕", "🍰", 150, "food", "丰盛的一餐，饱食度 +50", ""),
                item("pet-milk", "温牛奶", "🥛", 80, "food", "睡前喝一杯，心情 +20", ""),
                item("cosmetic-hat", "小草帽", "🎩", 100, "cosmetic", "遮阳又卖萌，农场主风格", ""),
                item("cosmetic-bow", "蝴蝶结", "🎀", 130, "cosmetic", "别在耳朵上，甜度爆表", ""),
                item("cosmetic-glasses", "酷墨镜", "🕶️", 150, "cosmetic", "戴上就是整条街最靓的崽", ""),
                item("cosmetic-crown", "小王冠", "👑", 400, "cosmetic", "王者气质，据说+100% 气场", ""),
                item("rename-card", "改名卡", "✏️", 100, "rename", "支持特殊符号的改名卡", ""),
                item("point-redpacket", "积分红包", "🧧", 80, "redpacket", "送给好友 50 积分，心意无价", ""),
                item("lucky-star", "幸运星", "⭐", 50, "lucky", "当天抽运势可以多一次", ""),
                item("title-collector", "图鉴收藏家称号", "👑", 500, "title", "集齐 22 只小马的证明", "")
        );
    }

    private static Map<String, Object> item(String id, String name, String icon, int price, String kind, String desc, String petKind) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("icon", icon);
        m.put("price", price);
        m.put("kind", kind);
        m.put("desc", desc);
        m.put("petKind", petKind);
        return m;
    }

    /**
     * 宠物种类归一化：旧版「龙崽 dragon」统一按「萨摩耶 dog」处理，
     * 这样老存档、老宠物蛋不用洗数据也能正常显示。
     */
    public static String normalizePetKind(String kind) {
        if (kind == null) {
            return "";
        }
        String k = kind.trim();
        if ("dragon".equalsIgnoreCase(k) || "puppy".equalsIgnoreCase(k)) {
            return "dog";
        }
        return k;
    }

    /** 宠物种类：14 种真实品种的 3D 毛绒小伙伴 */
    public static Map<String, Object> petKind(String kind) {
        String k = normalizePetKind(kind);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", k);
        switch (k) {
            case "cat":      m.put("name", "狸花猫");     m.put("emoji", "🐱"); break;
            case "dog":      m.put("name", "萨摩耶");     m.put("emoji", "🐶"); break;
            case "bird":     m.put("name", "玄凤鹦鹉");   m.put("emoji", "🦜"); break;
            case "rabbit":   m.put("name", "垂耳兔");     m.put("emoji", "🐰"); break;
            case "fox":      m.put("name", "赤狐");       m.put("emoji", "🦊"); break;
            case "bear":     m.put("name", "棕熊");       m.put("emoji", "🧸"); break;
            case "hamster":  m.put("name", "金丝熊");     m.put("emoji", "🐹"); break;
            case "penguin":  m.put("name", "小企鹅");     m.put("emoji", "🐧"); break;
            case "mouse":    m.put("name", "花枝鼠");     m.put("emoji", "🐭"); break;
            case "squirrel": m.put("name", "红松鼠");     m.put("emoji", "🐿️"); break;
            case "ragdoll":  m.put("name", "布偶猫");     m.put("emoji", "🐈"); break;
            case "corgi":    m.put("name", "柯基犬");     m.put("emoji", "🐕"); break;
            case "shiba":    m.put("name", "柴犬");       m.put("emoji", "🦮"); break;
            case "budgie":   m.put("name", "虎皮鹦鹉");   m.put("emoji", "🦜"); break;
            default:         m.put("name", "小宠物");     m.put("emoji", "🐾"); break;
        }
        return m;
    }

    /** 绑定关系定义：type → 名称与 emoji */
    public static Map<String, Object> relation(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type == null ? "" : type);
        switch (type == null ? "" : type) {
            case "bestie":
                m.put("name", "闺蜜");
                m.put("emoji", "💞");
                break;
            case "bro":
                m.put("name", "兄弟");
                m.put("emoji", "🤜");
                break;
            case "master":
                m.put("name", "师徒");
                m.put("emoji", "🎓");
                break;
            case "partner":
                m.put("name", "搭档");
                m.put("emoji", "🤝");
                break;
            case "rival":
                m.put("name", "对手");
                m.put("emoji", "⚔️");
                break;
            default:
                m.put("name", "");
                m.put("emoji", "");
        }
        return m;
    }

    /** 互动标识：按消息条数与好友天数计算 */
    public static List<Map<String, Object>> interactBadges(int msgCount, int friendDays, String relationType) {
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        if (msgCount >= 520) {
            out.add(badge("soulmate", "生死之交", "💘", "聊天超过 520 条"));
        } else if (msgCount >= 200) {
            out.add(badge("old-iron", "老铁", "🔩", "聊天超过 200 条"));
        } else if (msgCount >= 50) {
            out.add(badge("chatty", "话痨", "💬", "聊天超过 50 条"));
        } else if (msgCount >= 1) {
            out.add(badge("hello", "初见", "👋", "说过第一句话"));
        }
        if (friendDays >= 365) {
            out.add(badge("anniversary", "周年之谊", "🎂", "成为好友满一年"));
        } else if (friendDays >= 30) {
            out.add(badge("month", "满月之交", "🌙", "成为好友满一个月"));
        }
        if (relationType != null && !relationType.isEmpty()) {
            Map<String, Object> rel = relation(relationType);
            out.add(badge("bind-" + relationType, String.valueOf(rel.get("name")),
                    String.valueOf(rel.get("emoji")), "绑定了特殊关系"));
        }
        return out;
    }

    private static Map<String, Object> badge(String id, String name, String emoji, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("emoji", emoji);
        m.put("desc", desc);
        return m;
    }
}
