package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.PasswordUtil;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    /** 提示文案：注册页会按「密码」两个字把错误挂到密码框上 */
    private static final String MSG_PASSWORD_RULE = "密码至少 6 位，且要同时含字母和数字";

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> register(String name, String password) {
        name = normalizeName(name);
        if (name.isEmpty()) {
            return Result.fail("请输入小马名字");
        }
        if (name.length() < 2 || name.length() > 20) {
            return Result.fail("名字需 2-20 个字符");
        }
        if (password == null || password.length() < 6
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            return Result.fail(MSG_PASSWORD_RULE);
        }

        if (findByName(name) != null) {
            return Result.fail("这个小马名字已经被用过啦，换一个吧");
        }

        String salt = PasswordUtil.randomSalt();
        User user = new User();
        user.setName(name);
        user.setSalt(salt);
        user.setPassword(PasswordUtil.hash(salt, password));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        return Result.ok("注册成功", data);
    }

    @Override
    public Result<?> login(String name, String password) {
        name = normalizeName(name);
        if (name.isEmpty()) {
            return Result.fail("请输入小马名字");
        }
        if (password == null || password.isEmpty()) {
            return Result.fail("请输入密码");
        }

        User user = findByName(name);
        if (user == null) {
            return Result.fail("这个小马名字还没注册过，先去注册一个吧");
        }
        if (!PasswordUtil.matches(user.getSalt(), password, user.getPassword())) {
            return Result.fail("密码不正确，再想想～");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        return Result.ok("登录成功", data);
    }

    private User findByName(String name) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
