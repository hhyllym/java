package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分：余额、流水、排行榜、赠送给好友。
 */
@RestController
@RequestMapping("/api/point")
public class PointController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PointLogMapper pointLogMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private UserController userController;

    /** 积分余额 */
    @GetMapping("/balance")
    public Result<?> balance(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.fail("没有找到这只小马");
        }
        return Result.ok(u.getPoints() == null ? 0 : u.getPoints());
    }

    /** 最近 30 条积分流水 */
    @GetMapping("/logs")
    public Result<?> logs(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        List<PointLog> rows = pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId)
                .orderByDesc(PointLog::getId)
                .last("limit 30"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (PointLog l : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("amount", l.getAmount());
            m.put("biz", l.getBiz());
            m.put("remark", l.getRemark() == null ? "" : l.getRemark());
            m.put("time", String.valueOf(l.getCreateTime()));
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 积分榜 top 20 */
    @GetMapping("/rank")
    public Result<?> rank() {
        List<User> rows = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getPoints)
                .last("limit 20"));
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 1;
        for (User u : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", i++);
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
            m.put("points", u.getPoints() == null ? 0 : u.getPoints());
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 送积分给好友（消耗积分红包，双方各 +50 的语义：送方 -50、收方 +50） */
    @PostMapping("/gift")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> gift(@RequestBody Map<String, Object> body) {
        Long fromId = toLong(body.get("fromId"));
        Long toId = toLong(body.get("toId"));
        if (fromId == null || toId == null) {
            return Result.fail("参数不完整");
        }
        if (fromId.equals(toId)) {
            return Result.fail("不能送给自己哦");
        }
        User from = userMapper.selectById(fromId);
        User to = userMapper.selectById(toId);
        if (from == null || to == null) {
            return Result.fail("没有找到这只小马");
        }
        // 必须是好友
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(w -> w.eq(Friend::getUserId, fromId).eq(Friend::getFriendId, toId)
                        .or().eq(Friend::getUserId, toId).eq(Friend::getFriendId, fromId))
                .last("limit 1"));
        if (f == null) {
            return Result.fail("只能送给好友哦");
        }
        int mine = from.getPoints() == null ? 0 : from.getPoints();
        if (mine < 50) {
            return Result.fail("积分不够 50，送不出手");
        }
        changePoints(fromId, -50, "gift-out", "送给 " + to.getName() + " 的积分红包");
        userController.addPoints(toId, 50, "gift-in", "收到 " + from.getName() + " 的积分红包");
        return Result.ok("红包已送达，" + to.getName() + " 收到 50 积分 🧧");
    }

    private void changePoints(Long userId, int amount, String biz, String remark) {
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("points = GREATEST(0, IFNULL(points, 0) + " + amount + ")"));
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
