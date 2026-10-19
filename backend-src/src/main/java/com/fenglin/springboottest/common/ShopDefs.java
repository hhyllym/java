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

    /** 每日任务：id / 名称 / 描述 / 积分 / 上报类型 / 目标次数 */
    public static List<Map<String, Object>> dailyTasks() {
        return Arrays.asList(
                task("daily_login", "每日登录", "打开小马王国就算打卡", 5, "login", 1),
                task("daily_msg", "消息小能手", "给好友发送 3 条消息", 15, "msg", 3),
                task("daily_game", "游戏时间", "玩 1 局小游戏", 15, "game", 1),
                task("daily_view", "图鉴学者", "浏览 3 次小马图鉴", 10, "view", 3),
                task("daily_checkin", "学院打卡", "去学院完成打卡", 10, "checkin", 1)
        );
    }

    private static Map<String, Object> task(String id, String name, String desc, int points, String type, int target) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("desc", desc);
        m.put("points", points);
        m.put("type", type);
        m.put("target", target);
        return m;
    }

    /** 商店商品 */
    public static List<Map<String, Object>> shopItems() {
        return Arrays.asList(
                item("pet-egg-dragon", "小火龙蛋", "🥚", 200, "pet", "孵出来是喷小火苗的龙崽", "dragon"),
                item("pet-egg-cat", "奶糖猫蛋", "🐣", 200, "pet", "又甜又软的小猫，爱打呼噜", "cat"),
                item("pet-egg-bird", "云雀蛋", "🪺", 200, "pet", "嗓门很小的云雀，唱歌跑调", "bird"),
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

    /** 宠物种类 */
    public static Map<String, Object> petKind(String kind) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        if ("dragon".equals(kind)) {
            m.put("name", "小火龙");
            m.put("emoji", "🐲");
        } else if ("cat".equals(kind)) {
            m.put("name", "奶糖猫");
            m.put("emoji", "🐱");
        } else if ("bird".equals(kind)) {
            m.put("name", "云雀");
            m.put("emoji", "🐦");
        } else {
            m.put("name", "小宠物");
            m.put("emoji", "🐾");
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
