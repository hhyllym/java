package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息中心统一表 t_message。
 * type: BOARD 留言板（公开墙）/ PM 站内私信 / NOTIFY 系统通知
 * to_id: 私信=收件人 id；系统通知=收件人 id 或 -1（全员广播）；留言板=NULL
 * read_flag: 0 未读 1 已读（仅 PM 与「个人」系统通知使用；广播公告始终视为已读）
 */
@TableName("t_message")
public class Message implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String type;
    private Long fromId;
    private String fromName;
    private Long toId;
    private String toName;
    private String content;
    private Integer readFlag;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Integer readFlag) {
        this.readFlag = readFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
