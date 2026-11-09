package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表 t_user。
 * 注意：只有「小马名字 + 密码」，没有邮箱、没有手机号，别再往这里加。
 * 密码为明文存储（按需求不加密）；permission：1=默认普通用户，0=普通管理员，2=超级管理员。
 */
@TableName("t_user")
public class User implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 小马名字，登录账号，唯一 */
    @TableField("name")
    private String name;

    /** 头像 emoji（如 🐲🌙☀️），空则用默认 */
    @TableField("avatar")
    private String avatar;

    /** 积分余额 */
    @TableField("points")
    private Integer points;

    /** 登录密码（明文，不加密） */
    @TableField("password")
    private String password;

    /** 权限：1=默认普通用户（看不到后台入口）；0=普通管理员；2=超级管理员（可管理其他管理员权限） */
    @TableField("permission")
    private Integer permission;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPermission() {
        return permission;
    }

    public void setPermission(Integer permission) {
        this.permission = permission;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
