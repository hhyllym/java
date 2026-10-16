package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.Result;

/**
 * 注册 / 登录业务。
 * 只认「小马名字 + 密码」，不要邮箱、不要手机号。
 */
public interface UserService {

    /** 注册：name 2~20 字，password 至少 6 位且含字母和数字 */
    Result<?> register(String name, String password);

    /** 登录：名字不存在 / 密码不对都会给出能直接显示给用户的中文提示 */
    Result<?> login(String name, String password);

    /**
     * 账号资料（「我的账号」页用）：注册时间、加入天数，
     * 以及——如果名字或英文名正好和图鉴里某只小马对上——那只小马的完整卡片。
     */
    Result<?> profile(Long id);
}
