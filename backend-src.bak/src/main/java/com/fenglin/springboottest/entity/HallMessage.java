package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 星糖大厅公共聊天消息表 t_hall_message。
 * 大厅是「所有用户都能进」的公共空间，所以这里没有 to_id —— 发出去所有人都看得见。
 * user_id 为空表示系统公告（例如「XX 发起了一个游戏邀请」）。
 */
@TableName("t_hall_message")
public class HallMessage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发送者 id；系统公告为空 */
    @TableField("user_id")
    private Long userId;

    /** 发送者名字（冗余存一份，用户改名后老消息仍显示当时的名字） */
    @TableField("name")
    private String name;

    /** 发送者头像 emoji */
    @TableField("avatar")
    private String avatar;

    @TableField("content")
    private String content;

    /** text 普通发言 / system 系统提示 */
    @TableField("msg_type")
    private String msgType;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
