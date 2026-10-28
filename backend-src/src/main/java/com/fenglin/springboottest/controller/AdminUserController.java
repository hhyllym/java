package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 用户管理（t_user 里注册的小马账号）。
 *   GET    /api/admin/user/list           分页列表（可按名字搜）
 *   PUT    /api/admin/user/{id}/points    调整积分余额
 *   PUT    /api/admin/user/{id}/permission   调整权限等级（仅超级管理员 permission=2 可调用）
 *   DELETE /api/admin/user/{id}           删除账号（普通管理员不能删超级管理员）
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private UserMapper userMapper;

    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    /** 取当前登录管理员（含其权限等级），用于判断是否有「管理其他管理员」的权限 */
    private User currentAdmin(HttpServletRequest request) {
        String name = adminAuth.nameOf(request.getHeader(AdminAuth.HEADER));
        if (name == null) return null;
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "12") int size) {
        Result<?> a = auth(request);
        if (a != null) return a;
        String kw = keyword == null ? "" : keyword.trim();
        page = Math.max(1, page);
        size = Math.max(1, Math.min(50, size));

        LambdaQueryWrapper<User> countW = new LambdaQueryWrapper<>();
        if (!kw.isEmpty()) countW.like(User::getName, kw);
        long total = userMapper.selectCount(countW);

        LambdaQueryWrapper<User> listW = new LambdaQueryWrapper<>();
        if (!kw.isEmpty()) listW.like(User::getName, kw);
        listW.orderByDesc(User::getCreateTime).orderByAsc(User::getId)
                .last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<User> rows = userMapper.selectList(listW);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", rows);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", (total + size - 1) / Math.max(1, size));
        return Result.ok(data);
    }

    @PutMapping("/{id}/points")
    public Result<?> updatePoints(HttpServletRequest request,
                                 @PathVariable("id") Long id,
                                 @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (id == null) return Result.fail("缺少用户 id");
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail("找不到这个用户");
        Integer points = toInt(body.get("points"));
        if (points == null || points < 0) return Result.fail("积分必须是非负整数");
        u.setPoints(points);
        userMapper.updateById(u);
        return Result.ok("已将该用户积分设为 " + points, null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable("id") Long id) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (id == null) return Result.fail("缺少用户 id");
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail("找不到这个用户");
        // 普通管理员不能删除超级管理员；超级管理员也不能删除自己
        User me = currentAdmin(request);
        Integer myPerm = me == null || me.getPermission() == null ? 1 : me.getPermission();
        Integer targetPerm = u.getPermission() == null ? 1 : u.getPermission();
        if (targetPerm == 2) {
            if (myPerm != 2) return Result.fail("只有超级管理员可以删除超级管理员");
            if (me != null && me.getName().equals(u.getName())) return Result.fail("不能删除自己");
        }
        userMapper.deleteById(id);
        return Result.ok("已删除用户「" + u.getName() + "」", null);
    }

    @PutMapping("/{id}/permission")
    public Result<?> updatePermission(HttpServletRequest request,
                                      @PathVariable("id") Long id,
                                      @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        User me = currentAdmin(request);
        if (me == null) return Result.fail("未授权或登录已过期");
        // 只有超级管理员（permission=2）能调整其他账号的权限等级
        if (me.getPermission() == null || me.getPermission() != 2) {
            return Result.fail("只有超级管理员可以调整账号权限");
        }
        if (id == null) return Result.fail("缺少用户 id");
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail("找不到这个用户");
        // 不能修改自己的权限，避免把自己锁在后台外
        if (me.getName().equals(u.getName())) {
            return Result.fail("不能修改自己的权限");
        }
        Integer perm = toInt(body.get("permission"));
        if (perm == null || (perm != 0 && perm != 1 && perm != 2)) {
            return Result.fail("权限只能是 0（普通管理员）/ 1（普通用户）/ 2（超级管理员）");
        }
        u.setPermission(perm);
        userMapper.updateById(u);
        String label = perm == 2 ? "超级管理员" : (perm == 0 ? "普通管理员" : "普通用户");
        return Result.ok("已将「" + u.getName() + "」设为" + label, null);
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
