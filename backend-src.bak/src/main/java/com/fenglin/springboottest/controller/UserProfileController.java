package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 「我的账号」页接口：GET /api/user/profile?id=1
 * 返回注册时间、加入天数，以及同名的图鉴小马（没有就返回 null）。
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public Result<?> profile(@RequestParam(value = "id", required = false) Long id) {
        return userService.profile(id);
    }
}
