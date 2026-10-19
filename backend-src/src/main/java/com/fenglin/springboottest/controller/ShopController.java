package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.Pet;
import com.fenglin.springboottest.entity.PetCosmetic;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserItem;
import com.fenglin.springboottest.mapper.PetCosmeticMapper;
import com.fenglin.springboottest.mapper.PetMapper;
import com.fenglin.springboottest.mapper.UserItemMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分商店：商品列表、兑换、背包、使用物品。
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserItemMapper userItemMapper;

    @Autowired
    private PetMapper petMapper;

    @Autowired
    private PetCosmeticMapper petCosmeticMapper;

    @Autowired
    private UserController userController;

    /** 商品列表 */
    @GetMapping("/items")
    public Result<?> items() {
        return Result.ok(ShopDefs.shopItems());
    }

    /** 兑换商品 */
    @PostMapping("/buy")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> buy(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String itemId = body.get("itemId") == null ? "" : String.valueOf(body.get("itemId")).trim();
        if (userId == null || itemId.isEmpty()) {
            return Result.fail("参数不完整");
        }
        Map<String, Object> item = findItem(itemId);
        if (item == null) {
            return Result.fail("没有这个商品");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.fail("先登录再来逛商店吧");
        }
        int price = ((Number) item.get("price")).intValue();
        int mine = u.getPoints() == null ? 0 : u.getPoints();
        if (mine < price) {
            return Result.fail("积分不够，还差 " + (price - mine) + " 分");
        }

        // 扣积分
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("points = IFNULL(points, 0) - " + price));

        boolean isCosmetic = "cosmetic".equals(item.get("kind"));
        if (isCosmetic) {
            // 装扮进「衣橱」（t_pet_cosmetic），不走消耗品背包
            PetCosmetic owned = petCosmeticMapper.selectOne(new LambdaQueryWrapper<PetCosmetic>()
                    .eq(PetCosmetic::getUserId, userId)
                    .eq(PetCosmetic::getCosmeticId, itemId)
                    .last("limit 1"));
            if (owned == null) {
                owned = new PetCosmetic();
                owned.setUserId(userId);
                owned.setCosmeticId(itemId);
                owned.setCount(1);
                owned.setEquipped(0);
                owned.setCreateTime(LocalDateTime.now());
                petCosmeticMapper.insert(owned);
            } else {
                owned.setCount((owned.getCount() == null ? 0 : owned.getCount()) + 1);
                petCosmeticMapper.updateById(owned);
            }
            userController.addPoints(userId, 0, "shop", "兑换装扮：" + item.get("name") + " -" + price);
            return Result.ok("兑换成功：" + item.get("name") + "！去宠物面板的「衣橱」装扮吧");
        }

        // 入背包
        UserItem ui = userItemMapper.selectOne(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemId, itemId)
                .last("limit 1"));
        if (ui == null) {
            ui = new UserItem();
            ui.setUserId(userId);
            ui.setItemId(itemId);
            ui.setCount(1);
            ui.setUpdateTime(LocalDateTime.now());
            userItemMapper.insert(ui);
        } else {
            ui.setCount((ui.getCount() == null ? 0 : ui.getCount()) + 1);
            userItemMapper.updateById(ui);
        }
        userController.addPoints(userId, 0, "shop", "兑换：" + item.get("name") + " -" + price);
        return Result.ok("兑换成功：" + item.get("name") + "！去背包看看吧");
    }

    /** 我的背包 */
    @GetMapping("/bag")
    public Result<?> bag(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<UserItem> rows = userItemMapper.selectList(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .gt(UserItem::getCount, 0));
        List<Map<String, Object>> out = new ArrayList<>();
        for (UserItem ui : rows) {
            Map<String, Object> item = findItem(ui.getItemId());
            if (item == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>(item);
            m.put("count", ui.getCount() == null ? 0 : ui.getCount());
            out.add(m);
        }
        return Result.ok(out);
    }

    /**
     * 使用物品：
     * - pet 蛋 → 直接领养宠物（kind 见商品定义）
     * - food → 喂宠物（消耗 1 份）
     * - rename / redpacket → 返回 need 标记，前端走对应流程
     * - lucky → 当日运势 +1（返回 need 由前端记录）
     * - title → 已拥有即获得称号
     */
    @PostMapping("/use")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> use(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String itemId = body.get("itemId") == null ? "" : String.valueOf(body.get("itemId")).trim();
        String petName = body.get("petName") == null ? "" : String.valueOf(body.get("petName")).trim();
        if (userId == null || itemId.isEmpty()) {
            return Result.fail("参数不完整");
        }
        Map<String, Object> item = findItem(itemId);
        if (item == null) {
            return Result.fail("没有这个物品");
        }
        // 检查持有
        UserItem ui = userItemMapper.selectOne(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemId, itemId)
                .last("limit 1"));
        if (ui == null || ui.getCount() == null || ui.getCount() <= 0) {
            return Result.fail("背包里还没有这个物品");
        }

        String kind = String.valueOf(item.get("kind"));
        switch (kind) {
            case "pet": {
                // 领养宠物
                Pet exists = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                        .eq(Pet::getUserId, userId).last("limit 1"));
                if (exists != null) {
                    return Result.fail("你已经有一只宠物了，对它好一点！");
                }
                Pet p = new Pet();
                p.setUserId(userId);
                p.setKind(String.valueOf(item.get("petKind")));
                p.setName(petName.isEmpty() ? String.valueOf(ShopDefs.petKind(String.valueOf(item.get("petKind"))).get("name")) : petName);
                p.setLevel(1);
                p.setExp(0);
                p.setHunger(60);
                p.setMood(80);
                p.setCreateTime(LocalDateTime.now());
                p.setUpdateTime(LocalDateTime.now());
                petMapper.insert(p);
                consume(ui);
                return Result.ok("🎉 宠物蛋孵化了，快去照顾它吧！");
            }
            case "food": {
                Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                        .eq(Pet::getUserId, userId).last("limit 1"));
                if (p == null) {
                    return Result.fail("你还没有宠物，先去兑换一枚宠物蛋吧");
                }
                consume(ui);
                // 不同食物不同效果：口粮饱食+25 / 蛋糕饱食+50 / 牛奶心情+20，都附赠经验
                int hunger = p.getHunger() == null ? 50 : p.getHunger();
                int mood = p.getMood() == null ? 80 : p.getMood();
                String effect;
                if ("pet-cake".equals(itemId)) {
                    hunger = Math.min(100, hunger + 50);
                    effect = "丰盛大餐，饱食度 +50";
                } else if ("pet-milk".equals(itemId)) {
                    mood = Math.min(100, mood + 20);
                    effect = "温牛奶下肚，心情 +20";
                } else {
                    hunger = Math.min(100, hunger + 25);
                    effect = "饱食度 +25";
                }
                p.setHunger(hunger);
                p.setMood(mood);
                p.setExp((p.getExp() == null ? 0 : p.getExp()) + 10);
                p.setUpdateTime(LocalDateTime.now());
                petMapper.updateById(p);
                userController.addPoints(userId, 2, "pet", "喂养宠物");
                return Result.ok(p.getName() + " 吃得很开心！" + effect);
            }
            case "rename": {
                // 改名卡：直接用新名字改名，然后消耗一张卡
                String newName = body.get("newName") == null ? "" : String.valueOf(body.get("newName")).trim();
                if (newName.isEmpty()) {
                    return Result.fail("名字不能是空白的");
                }
                if (newName.length() > 20) {
                    return Result.fail("名字最长 20 个字符，特殊符号也算哦");
                }
                User me = userMapper.selectById(userId);
                if (me == null) {
                    return Result.fail("先登录再来使用吧");
                }
                if (!newName.equals(me.getName())) {
                    User dup = userMapper.selectOne(new LambdaQueryWrapper<User>()
                            .eq(User::getName, newName).last("limit 1"));
                    if (dup != null) {
                        return Result.fail("这个名字已经被别的小马用了，换一个吧");
                    }
                }
                me.setName(newName);
                userMapper.updateById(me);
                consume(ui);
                return Result.ok("✏️ 改名成功，以后就叫「" + newName + "」啦");
            }
            case "redpacket": {
                // 积分红包：送好友 50 积分，然后消耗一个红包
                Long toId = toLong(body.get("toId"));
                if (toId == null) {
                    return Result.fail("选一位好友再送吧");
                }
                if (toId.equals(userId)) {
                    return Result.fail("红包不能送给自己哦");
                }
                User to = userMapper.selectById(toId);
                if (to == null) {
                    return Result.fail("没有找到收红包的小马");
                }
                userController.addPoints(toId, 50, "redpacket", "收到积分红包");
                consume(ui);
                return Result.ok("🧧 50 积分红包已送给 " + to.getName() + "，心意无价！");
            }
            case "lucky":
                consume(ui);
                return Result.ok("⭐ 幸运星闪耀！今天可以多抽一次运势");
            case "title":
                consume(ui);
                return Result.ok("👑 称号「图鉴收藏家」已佩戴！");
            default:
                return Result.fail("这个物品还不能使用");
        }
    }

    private Map<String, Object> needRename() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("need", "rename");
        return m;
    }

    private Map<String, Object> needRedpacket() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("need", "redpacket");
        return m;
    }

    private void consume(UserItem ui) {
        ui.setCount((ui.getCount() == null ? 0 : ui.getCount()) - 1);
        userItemMapper.updateById(ui);
    }

    private Map<String, Object> findItem(String itemId) {
        for (Map<String, Object> item : ShopDefs.shopItems()) {
            if (itemId.equals(item.get("id"))) {
                return item;
            }
        }
        return null;
    }

    private Long toLong(Object o) {
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
