package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.ShopDefs;
import com.fenglin.springboottest.entity.TaskProgress;
import com.fenglin.springboottest.mapper.TaskProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日任务：列表、上报进度、领奖。
 *
 * 13 个任务里，前 12 个由前端在对应操作成功后调用 /bump 推进；
 * 最后一个「全勤宝箱」type=bonus，禁止外部上报，只能由本类在
 * 其它任务全部领奖后自动解锁，避免刷分。
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    private static final String BONUS_TYPE = "bonus";

    @Autowired
    private TaskProgressMapper taskProgressMapper;

    @Autowired
    private UserController userController;

    private String today() {
        return LocalDate.now().toString();
    }

    private boolean isClaimed(TaskProgress p) {
        return p != null && p.getClaimed() != null && p.getClaimed() == 1;
    }

    private boolean isBonus(Map<String, Object> def) {
        return BONUS_TYPE.equals(def.get("type"));
    }

    private int targetOf(Map<String, Object> def) {
        return ((Number) def.get("target")).intValue();
    }

    /** 当天该用户的全部进度行，按 taskId 索引 */
    private Map<String, TaskProgress> progressMap(Long userId, String day) {
        List<TaskProgress> rows = taskProgressMapper.selectList(new LambdaQueryWrapper<TaskProgress>()
                .eq(TaskProgress::getUserId, userId)
                .eq(TaskProgress::getDay, day));
        Map<String, TaskProgress> byId = new LinkedHashMap<>();
        for (TaskProgress t : rows) {
            byId.put(t.getTaskId(), t);
        }
        return byId;
    }

    /** 今日任务列表（带进度、领奖状态、宝箱解锁提示） */
    @GetMapping("/today")
    public Result<?> today(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        String day = today();
        Map<String, TaskProgress> byId = progressMap(userId, day);
        List<Map<String, Object>> defs = ShopDefs.dailyTasks();

        // 宝箱解锁条件：除 bonus 外的任务全部已领取
        int normalCount = 0;
        int normalClaimed = 0;
        for (Map<String, Object> def : defs) {
            if (isBonus(def)) continue;
            normalCount++;
            if (isClaimed(byId.get(String.valueOf(def.get("id"))))) normalClaimed++;
        }
        int need = Math.max(0, normalCount - normalClaimed);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            String id = String.valueOf(def.get("id"));
            TaskProgress p = byId.get(id);
            Map<String, Object> m = new LinkedHashMap<>(def);
            int target = targetOf(def);
            int progress = p == null || p.getProgress() == null ? 0 : p.getProgress();
            m.put("progress", Math.min(progress, target));
            m.put("claimed", isClaimed(p));
            m.put("done", progress >= target);
            if (isBonus(def)) {
                m.put("need", need);
                m.put("unlocked", need == 0);
            }
            out.add(m);
        }
        return Result.ok(out);
    }

    /**
     * 上报进度。type 对应 ShopDefs 里的任务类型，一次可推进多个同类型任务。
     * - 已领奖的任务不再累加，进度封顶在 target，避免数据库里堆出无意义的数字；
     * - bonus 类型不接受外部上报，只能由领奖流程解锁。
     */
    @PostMapping("/bump")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> bump(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String type = body.get("type") == null ? "" : String.valueOf(body.get("type")).trim();
        int n = body.get("n") == null ? 1 : ((Number) body.get("n")).intValue();
        if (userId == null || type.isEmpty()) {
            return Result.fail("参数不完整");
        }
        if (n < 1) n = 1;
        if (n > 100) n = 100;
        if (BONUS_TYPE.equals(type)) {
            return Result.ok("宝箱要靠领奖解锁哦");
        }

        String day = today();
        List<Map<String, Object>> defs = ShopDefs.dailyTasks();
        Map<String, TaskProgress> byId = progressMap(userId, day);
        boolean changed = false;

        for (Map<String, Object> def : defs) {
            if (isBonus(def) || !type.equals(def.get("type"))) continue;
            String id = String.valueOf(def.get("id"));
            int target = targetOf(def);
            TaskProgress p = byId.get(id);
            if (isClaimed(p)) continue;

            if (p == null) {
                p = new TaskProgress();
                p.setUserId(userId);
                p.setDay(day);
                p.setTaskId(id);
                p.setProgress(Math.min(target, n));
                p.setClaimed(0);
                taskProgressMapper.insert(p);
            } else {
                int cur = p.getProgress() == null ? 0 : p.getProgress();
                int next = Math.min(target, cur + n);
                if (next == cur) continue;
                p.setProgress(next);
                taskProgressMapper.updateById(p);
            }
            changed = true;
        }
        return Result.ok(changed ? "进度已更新" : "已收到");
    }

    /** 领奖：任务完成且未领 → 发积分；领完普通任务自动解锁全勤宝箱 */
    @PostMapping("/claim")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> claim(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String taskId = body.get("taskId") == null ? "" : String.valueOf(body.get("taskId")).trim();
        if (userId == null || taskId.isEmpty()) {
            return Result.fail("参数不完整");
        }
        List<Map<String, Object>> defs = ShopDefs.dailyTasks();
        Map<String, Object> def = null;
        Map<String, Object> bonusDef = null;
        for (Map<String, Object> d : defs) {
            if (taskId.equals(d.get("id"))) def = d;
            if (isBonus(d)) bonusDef = d;
        }
        if (def == null) {
            return Result.fail("没有这个任务");
        }

        String day = today();
        TaskProgress p = taskProgressMapper.selectOne(new LambdaQueryWrapper<TaskProgress>()
                .eq(TaskProgress::getUserId, userId)
                .eq(TaskProgress::getDay, day)
                .eq(TaskProgress::getTaskId, taskId)
                .last("limit 1"));
        if (p == null) {
            return Result.fail("还没完成这个任务");
        }
        int target = targetOf(def);
        int progress = p.getProgress() == null ? 0 : p.getProgress();
        if (progress < target) {
            return Result.fail("任务还没完成，再努努力");
        }
        // 带 claimed=0 条件更新，防止两个人同时点领取时重复发积分
        int rows = taskProgressMapper.update(null, new LambdaUpdateWrapper<TaskProgress>()
                .eq(TaskProgress::getId, p.getId())
                .eq(TaskProgress::getClaimed, 0)
                .set(TaskProgress::getClaimed, 1));
        if (rows == 0) {
            return Result.fail("这个奖励今天已经领过啦");
        }

        int points = ((Number) def.get("points")).intValue();
        userController.addPoints(userId, points, "task", "任务奖励：" + def.get("name"));

        // 非宝箱任务领完后，检查是否解锁全勤宝箱
        if (!isBonus(def) && bonusDef != null) {
            Map<String, TaskProgress> byId = progressMap(userId, day);
            int normalCount = 0;
            int normalClaimed = 0;
            for (Map<String, Object> d : defs) {
                if (isBonus(d)) continue;
                normalCount++;
                String id = String.valueOf(d.get("id"));
                TaskProgress q = id.equals(taskId) ? null : byId.get(id);
                // 刚领的这个在 progressMap 里可能还是旧值，直接算作已领取
                if (id.equals(taskId) || isClaimed(q)) normalClaimed++;
            }
            if (normalCount > 0 && normalClaimed >= normalCount) {
                String bonusId = String.valueOf(bonusDef.get("id"));
                TaskProgress bp = byId.get(bonusId);
                if (!isClaimed(bp)) {
                    if (bp == null) {
                        bp = new TaskProgress();
                        bp.setUserId(userId);
                        bp.setDay(day);
                        bp.setTaskId(bonusId);
                        bp.setProgress(1);
                        bp.setClaimed(0);
                        taskProgressMapper.insert(bp);
                    } else if (bp.getProgress() == null || bp.getProgress() < 1) {
                        bp.setProgress(1);
                        taskProgressMapper.updateById(bp);
                    }
                }
            }
        }
        return Result.ok("领取成功，+" + points + " 积分！");
    }

    private Long toLong(Object o) {
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
