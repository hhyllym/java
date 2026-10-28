package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.MessageMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    /** 提示文案：注册页会按「密码」两个字把错误挂到密码框上 */
    private static final String MSG_PASSWORD_RULE = "密码至少 6 位，且要同时含字母和数字";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private PonyService ponyService;

    @Override
    public Result<?> profile(Long id) {
        if (id == null) {
            return Result.fail("缺少账号 id");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail("没有找到这个账号，重新登录试试");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());

        LocalDateTime ct = user.getCreateTime();
        if (ct == null) {
            data.put("createTime", "");
            data.put("days", 1L);
        } else {
            data.put("createTime", ct.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            // 加入天数：注册当天算第 1 天
            data.put("days", Math.max(1L, ChronoUnit.DAYS.between(ct.toLocalDate(), LocalDate.now()) + 1));
        }

        // 名字（或英文名）和图鉴条目对上的话，把那只小马一起带回去
        data.put("pony", ponyService.findByName(user.getName()));
        return Result.ok(data);
    }

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

        User user = new User();
        user.setName(name);
        user.setPassword(password);   // 明文存储，按需求不加密
        user.setPermission(1);       // 注册用户默认权限：1=普通用户
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 注册成功，写入一条「个人」系统欢迎通知（to_id = 新用户；区别于 to_id=-1 的全员广播）
        Message welcome = new Message();
        welcome.setType("NOTIFY");
        welcome.setFromName("小马王国");
        welcome.setToId(user.getId());
        welcome.setContent("欢迎来到小马王国，" + name + "！记得去首页抽你的「今日幸运小马」🎲");
        welcome.setReadFlag(0);
        welcome.setCreateTime(LocalDateTime.now());
        messageMapper.insert(welcome);

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
        if (!password.equals(user.getPassword())) {
            return Result.fail("密码不正确，再想想～");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        // 把权限带回前端，首页据此决定是否展示「后台管理」入口
        data.put("permission", user.getPermission() == null ? 1 : user.getPermission());
        return Result.ok("登录成功", data);
    }

    private User findByName(String name) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getName, name).last("limit 1"));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
