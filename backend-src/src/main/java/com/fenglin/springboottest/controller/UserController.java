package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.PointLog;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.PointLogMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号与关系：资料（头像/改名）、绑定关系（互送积分）。
 * 路径用 /api/account，避开已有的 /api/user/profile（我的账号页在用）。
 * bean 名显式指定，避开脚手架里同名的旧 UserController。
 */
@RestController("userCenterController")
@RequestMapping("/api/account")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private PointLogMapper pointLogMapper;

    /** 个人资料 */
    @GetMapping("/profile")
    public Result<?> profile(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.fail("没有找到这只小马");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        m.put("points", u.getPoints() == null ? 0 : u.getPoints());
        return Result.ok(m);
    }

    /** 改头像 / 改名字（改名支持特殊符号，长度 1~20，不与别人重名） */
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        if (userId == null) {
            return Result.fail("先登录再来改资料吧");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.fail("没有找到这只小马");
        }

        // 换头像
        if (body.get("avatar") != null) {
            String avatar = String.valueOf(body.get("avatar")).trim();
            if (avatar.length() > 16) {
                avatar = avatar.substring(0, 16);
            }
            u.setAvatar(avatar);
        }

        // 改名字：非空、不超长、不含首尾空格、不重名；特殊符号随便加
        if (body.get("name") != null) {
            String name = String.valueOf(body.get("name"));
            if (name.length() > 20) {
                return Result.fail("名字最长 20 个字符，特殊符号也算哦");
            }
            name = name.trim();
            if (name.isEmpty()) {
                return Result.fail("名字不能是空白的");
            }
            if (!name.equals(u.getName())) {
                User dup = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getName, name).last("limit 1"));
                if (dup != null) {
                    return Result.fail("这个名字已经被别的小马用了，换一个吧");
                }
            }
            u.setName(name);
        }

        userMapper.updateById(u);
        return Result.ok("资料已更新");
    }

    /** 绑定特殊关系：bestie 闺蜜 / bro 兄弟 / master 师徒 / partner 搭档 / rival 对手 */
    @PostMapping("/bind")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> bind(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Long friendId = toLong(body.get("friendId"));
        String type = body.get("type") == null ? "" : String.valueOf(body.get("type")).trim();
        if (userId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        if (userId.equals(friendId)) {
            return Result.fail("不能和自己绑定哦");
        }
        if (!type.matches("bestie|bro|master|partner|rival")) {
            return Result.fail("不支持这种关系类型");
        }

        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId))
                .last("limit 1"));
        if (f == null) {
            return Result.fail("你们还不是好友，先加好友吧");
        }

        boolean firstBind = f.getRelationType() == null || f.getRelationType().isEmpty();
        f.setRelationType(type);
        f.setUpdateTime(LocalDateTime.now());
        friendMapper.updateById(f);

        // 首次绑定：双方各送 50 积分
        if (firstBind) {
            addPoints(userId, 50, "bind", "绑定「" + ShopDefs.relation(type).get("name") + "」关系奖励");
            addPoints(friendId, 50, "bind", "绑定「" + ShopDefs.relation(type).get("name") + "」关系奖励");
            return Result.ok("绑定成功！双方各获得 50 积分 🎉");
        }
        return Result.ok("关系已更新");
    }

    /** 解除绑定 */
    @PostMapping("/unbind")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> unbind(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        Long friendId = toLong(body.get("friendId"));
        if (userId == null || friendId == null) {
            return Result.fail("参数不完整");
        }
        friendMapper.update(null, new LambdaUpdateWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                        .or().eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId))
                .set(Friend::getRelationType, ""));
        return Result.ok("已解除绑定");
    }

    /** 给用户加积分并记流水（事务内调用） */
    public void addPoints(Long userId, int amount, String biz, String remark) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("points = IFNULL(points, 0) + " + amount));
        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setAmount(amount);
        log.setBiz(biz);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        pointLogMapper.insert(log);
    }

    private Long toLong(Object o) {
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
