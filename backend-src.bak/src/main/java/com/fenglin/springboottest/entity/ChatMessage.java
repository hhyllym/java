package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友聊天消息表 t_chat_message。
 * from_id / to_id 都是 t_user.id；read_flag 以接收方视角：0 未读 1 已读。
 */
@TableName("t_chat_message")
public class ChatMessage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("from_id")
    private Long fromId;

    @TableField("to_id")
    private Long toId;

    @TableField("content")
    private String content;

    /** 消息类型：text 文本 / image 图片(base64) / audio 语音(base64) / call 通话记录 */
    @TableField("msg_type")
    private String msgType;

    /** 0=正常 1=已撤回 */
    @TableField("recalled")
    private Integer recalled;

    /** 扩展字段：通话记录存时长秒数 */
    @TableField("extra")
    private String extra;

    @TableField("read_flag")
    private Integer readFlag;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
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

    public Integer getRecalled() {
        return recalled;
    }

    public void setRecalled(Integer recalled) {
        this.recalled = recalled;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
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
