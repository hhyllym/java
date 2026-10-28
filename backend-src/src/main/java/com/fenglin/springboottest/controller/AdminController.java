package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台管理系统 · 登录（复用小马王国用户表 t_user）。
 *   POST /api/admin/login  { username, password } → { token, name }
 *   GET  /api/admin/me     校验令牌，返回当前管理员名字
 * 只有 t_user 中 permission=0（普通管理员）或 2（超级管理员）的账号能登录后台。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminAuth adminAuth;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body == null ? null : body.get("username");
        String password = body == null ? null : body.get("password");
        if (username == null || password == null || username.trim().isEmpty() || password.isEmpty()) {
            return Result.fail("请输入账号和密码");
        }
        username = username.trim();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, username).last("limit 1"));
        if (user == null) {
            return Result.fail("账号或密码错误");
        }
        // 权限校验：permission=1 的普通用户不能进后台；0=普通管理员、2=超级管理员 都可进
        Integer perm = user.getPermission() == null ? 1 : user.getPermission();
        if (perm == 1) {
            return Result.fail("该账号没有后台管理权限");
        }
        if (!password.equals(user.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        String token = adminAuth.issueToken(user.getName());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("name", user.getName());
        data.put("permission", perm);
        return Result.ok(data);
    }

    @GetMapping("/me")
    public Result<?> me(HttpServletRequest request) {
        String token = request.getHeader(AdminAuth.HEADER);
        String name = adminAuth.nameOf(token);
        if (name == null || !adminAuth.isValid(token)) {
            return Result.fail("未授权或登录已过期");
        }
        // 返回当前管理员的权限等级，前端据此决定是否展示「管理其他管理员」的操作
        User u = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
        Integer perm = u == null || u.getPermission() == null ? 1 : u.getPermission();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("permission", perm); // 0=普通管理员，2=超级管理员
        return Result.ok(data);
    }
}
