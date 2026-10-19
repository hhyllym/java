package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.Pet;
import com.fenglin.springboottest.entity.PetCosmetic;
import com.fenglin.springboottest.entity.UserItem;
import com.fenglin.springboottest.mapper.PetCosmeticMapper;
import com.fenglin.springboottest.mapper.PetMapper;
import com.fenglin.springboottest.mapper.UserItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 宠物：查看、领养、喂食、玩耍、改名。
 */
@RestController
@RequestMapping("/api/pet")
public class PetController {

    @Autowired
    private PetMapper petMapper;

    @Autowired
    private UserItemMapper userItemMapper;

    @Autowired
    private PetCosmeticMapper petCosmeticMapper;

    @Autowired
    private UserController userController;

    /** 我的宠物（没有则返回 null） */
    @GetMapping("/mine")
    public Result<?> mine(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (p == null) {
            return Result.ok(null);
        }
        return Result.ok(toVO(p));
    }

    /** 领养（通过宠物蛋使用入口，这里兜底直接领） */
    @PostMapping("/adopt")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> adopt(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String kind = body.get("kind") == null ? "" : String.valueOf(body.get("kind")).trim();
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
        if (userId == null || kind.isEmpty()) {
            return Result.fail("参数不完整");
        }
        Pet exists = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (exists != null) {
            return Result.fail("你已经有一只宠物了，对它好一点！");
        }
        Pet p = new Pet();
        p.setUserId(userId);
        p.setKind(kind);
        p.setName(name.isEmpty() ? String.valueOf(ShopDefs.petKind(kind).get("name")) : name);
        p.setLevel(1);
        p.setExp(0);
        p.setHunger(60);
        p.setMood(80);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        petMapper.insert(p);
        userController.addPoints(userId, 20, "pet", "领养宠物奖励");
        return Result.ok("领养成功！+20 积分，快去照顾它吧");
    }

    /** 喂食：消耗 1 份口粮，饱食度 +25，经验 +10 */
    @PostMapping("/feed")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> feed(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        if (userId == null) {
            return Result.fail("参数不完整");
        }
        Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (p == null) {
            return Result.fail("你还没有宠物");
        }
        UserItem food = userItemMapper.selectOne(new LambdaQueryWrapper<UserItem>()
                .eq(UserItem::getUserId, userId)
                .eq(UserItem::getItemId, "pet-food")
                .last("limit 1"));
        if (food == null || food.getCount() == null || food.getCount() <= 0) {
            return Result.fail("口粮不够了，去商店兑换一点吧");
        }
        food.setCount(food.getCount() - 1);
        userItemMapper.updateById(food);

        p.setHunger(clamp((p.getHunger() == null ? 50 : p.getHunger()) + 25));
        p.setExp((p.getExp() == null ? 0 : p.getExp()) + 10);
        p.setUpdateTime(LocalDateTime.now());
        levelUp(p);
        petMapper.updateById(p);
        userController.addPoints(userId, 2, "pet", "喂食宠物");
        return Result.ok(p.getName() + " 吃得很开心！饱食度 +25");
    }

    /** 玩耍：心情 +15，经验 +10 */
    @PostMapping("/play")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> play(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        if (userId == null) {
            return Result.fail("参数不完整");
        }
        Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (p == null) {
            return Result.fail("你还没有宠物");
        }
        p.setMood(clamp((p.getMood() == null ? 80 : p.getMood()) + 15));
        p.setExp((p.getExp() == null ? 0 : p.getExp()) + 10);
        p.setUpdateTime(LocalDateTime.now());
        levelUp(p);
        petMapper.updateById(p);
        userController.addPoints(userId, 2, "pet", "陪宠物玩耍");
        return Result.ok("和 " + p.getName() + " 玩了一会儿，它超开心！");
    }

    /** 宠物改名 */
    @PostMapping("/rename")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rename(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
        if (userId == null || name.isEmpty()) {
            return Result.fail("名字不能是空白的");
        }
        if (name.length() > 20) {
            return Result.fail("名字最长 20 个字符");
        }
        Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (p == null) {
            return Result.fail("你还没有宠物");
        }
        p.setName(name);
        p.setUpdateTime(LocalDateTime.now());
        petMapper.updateById(p);
        return Result.ok("宠物改名为「" + name + "」");
    }

    /** 我的衣橱：拥有的装扮列表（含是否装备中） */
    @GetMapping("/cosmetics")
    public Result<?> cosmetics(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<PetCosmetic> rows = petCosmeticMapper.selectList(new LambdaQueryWrapper<PetCosmetic>()
                .eq(PetCosmetic::getUserId, userId)
                .gt(PetCosmetic::getCount, 0));
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (PetCosmetic c : rows) {
            Map<String, Object> item = findShopItem(c.getCosmeticId());
            if (item == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>(item);
            m.put("count", c.getCount() == null ? 0 : c.getCount());
            m.put("equipped", c.getEquipped() != null && c.getEquipped() == 1);
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 装备装扮（同时卸下其它，只能戴一件） */
    @PostMapping("/cosmetic/equip")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> equip(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String cosmeticId = body.get("cosmeticId") == null ? "" : String.valueOf(body.get("cosmeticId")).trim();
        if (userId == null || cosmeticId.isEmpty()) {
            return Result.fail("参数不完整");
        }
        Pet p = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId).last("limit 1"));
        if (p == null) {
            return Result.fail("你还没有宠物，先领养一只吧");
        }
        PetCosmetic owned = petCosmeticMapper.selectOne(new LambdaQueryWrapper<PetCosmetic>()
                .eq(PetCosmetic::getUserId, userId)
                .eq(PetCosmetic::getCosmeticId, cosmeticId)
                .last("limit 1"));
        if (owned == null || owned.getCount() == null || owned.getCount() <= 0) {
            return Result.fail("衣橱里还没有这个装扮，去商店兑换吧");
        }
        // 先全部卸下，再装备这一件
        petCosmeticMapper.update(null, new LambdaUpdateWrapper<PetCosmetic>()
                .eq(PetCosmetic::getUserId, userId)
                .set(PetCosmetic::getEquipped, 0));
        owned.setEquipped(1);
        petCosmeticMapper.updateById(owned);
        return Result.ok("已装扮：" + owned.getCosmeticId());
    }

    /** 卸下当前装扮 */
    @PostMapping("/cosmetic/unequip")
    public Result<?> unequip(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        if (userId == null) {
            return Result.fail("参数不完整");
        }
        petCosmeticMapper.update(null, new LambdaUpdateWrapper<PetCosmetic>()
                .eq(PetCosmetic::getUserId, userId)
                .eq(PetCosmetic::getEquipped, 1)
                .set(PetCosmetic::getEquipped, 0));
        return Result.ok("已卸下装扮");
    }

    /** 经验够了就升级 */
    private void levelUp(Pet p) {
        int level = p.getLevel() == null ? 1 : p.getLevel();
        int exp = p.getExp() == null ? 0 : p.getExp();
        boolean leveled = false;
        while (exp >= level * 50) {
            exp -= level * 50;
            level += 1;
            leveled = true;
        }
        if (leveled) {
            p.setLevel(level);
            p.setExp(exp);
            userController.addPoints(p.getUserId(), 20, "pet", "宠物升到 " + level + " 级");
        } else {
            p.setExp(exp);
        }
    }

    private Map<String, Object> toVO(Pet p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", p.getKind());
        m.put("name", p.getName());
        m.put("level", p.getLevel() == null ? 1 : p.getLevel());
        m.put("exp", p.getExp() == null ? 0 : p.getExp());
        m.put("hunger", clamp(p.getHunger() == null ? 50 : p.getHunger()));
        m.put("mood", clamp(p.getMood() == null ? 80 : p.getMood()));
        m.put("kindInfo", ShopDefs.petKind(p.getKind()));
        // 当前装扮（没有则为 null）
        PetCosmetic eq = petCosmeticMapper.selectOne(new LambdaQueryWrapper<PetCosmetic>()
                .eq(PetCosmetic::getUserId, p.getUserId())
                .eq(PetCosmetic::getEquipped, 1)
                .last("limit 1"));
        if (eq != null) {
            Map<String, Object> item = findShopItem(eq.getCosmeticId());
            if (item != null) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("id", item.get("id"));
                c.put("name", item.get("name"));
                c.put("icon", item.get("icon"));
                m.put("cosmetic", c);
            }
        }
        return m;
    }

    /** 从商店定义里找商品（装扮用的就是商店商品） */
    private Map<String, Object> findShopItem(String itemId) {
        for (Map<String, Object> item : ShopDefs.shopItems()) {
            if (itemId.equals(item.get("id"))) {
                return item;
            }
        }
        return null;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private Long toLong(Object o) {
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
