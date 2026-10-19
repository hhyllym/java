package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskProgressMapper taskProgressMapper;

    @Autowired
    private UserController userController;

    private String today() {
        return LocalDate.now().toString();
    }

    /** 今日任务列表（带进度与领奖状态） */
    @GetMapping("/today")
    public Result<?> today(@RequestParam Long userId) {
        if (userId == null) {
            return Result.fail("先登录再来查看吧");
        }
        String day = today();
        List<TaskProgress> rows = taskProgressMapper.selectList(new LambdaQueryWrapper<TaskProgress>()
                .eq(TaskProgress::getUserId, userId)
                .eq(TaskProgress::getDay, day));
        Map<String, TaskProgress> byId = new LinkedHashMap<>();
        for (TaskProgress t : rows) {
            byId.put(t.getTaskId(), t);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> def : ShopDefs.dailyTasks()) {
            String id = String.valueOf(def.get("id"));
            TaskProgress p = byId.get(id);
            Map<String, Object> m = new LinkedHashMap<>(def);
            int progress = p == null || p.getProgress() == null ? 0 : p.getProgress();
            int target = ((Number) def.get("target")).intValue();
            m.put("progress", Math.min(progress, target));
            m.put("claimed", p != null && p.getClaimed() != null && p.getClaimed() == 1);
            m.put("done", progress >= target);
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 上报进度：type = login / msg / game / view / checkin */
    @PostMapping("/bump")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> bump(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String type = body.get("type") == null ? "" : String.valueOf(body.get("type")).trim();
        int n = body.get("n") == null ? 1 : ((Number) body.get("n")).intValue();
        if (userId == null || type.isEmpty()) {
            return Result.fail("参数不完整");
        }

        String day = today();
        List<Map<String, Object>> defs = ShopDefs.dailyTasks();
        List<String> hitIds = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            if (type.equals(def.get("type"))) {
                hitIds.add(String.valueOf(def.get("id")));
            }
        }
        if (hitIds.isEmpty()) {
            return Result.ok("已收到");
        }

        for (String id : hitIds) {
            TaskProgress p = taskProgressMapper.selectOne(new LambdaQueryWrapper<TaskProgress>()
                    .eq(TaskProgress::getUserId, userId)
                    .eq(TaskProgress::getDay, day)
                    .eq(TaskProgress::getTaskId, id)
                    .last("limit 1"));
            if (p == null) {
                p = new TaskProgress();
                p.setUserId(userId);
                p.setDay(day);
                p.setTaskId(id);
                p.setProgress(n);
                p.setClaimed(0);
                taskProgressMapper.insert(p);
            } else {
                p.setProgress((p.getProgress() == null ? 0 : p.getProgress()) + n);
                taskProgressMapper.updateById(p);
            }
        }
        return Result.ok("进度已更新");
    }

    /** 领奖：任务完成且未领 → 发积分 */
    @PostMapping("/claim")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> claim(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        String taskId = body.get("taskId") == null ? "" : String.valueOf(body.get("taskId")).trim();
        if (userId == null || taskId.isEmpty()) {
            return Result.fail("参数不完整");
        }
        Map<String, Object> def = null;
        for (Map<String, Object> d : ShopDefs.dailyTasks()) {
            if (taskId.equals(d.get("id"))) {
                def = d;
                break;
            }
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
        int target = ((Number) def.get("target")).intValue();
        int progress = p.getProgress() == null ? 0 : p.getProgress();
        if (progress < target) {
            return Result.fail("任务还没完成，再努努力");
        }
        if (p.getClaimed() != null && p.getClaimed() == 1) {
            return Result.fail("这个奖励今天已经领过啦");
        }
        p.setClaimed(1);
        taskProgressMapper.updateById(p);

        int points = ((Number) def.get("points")).intValue();
        userController.addPoints(userId, points, "task", "任务奖励：" + def.get("name"));
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
