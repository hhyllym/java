package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/** 每日任务进度 t_task_progress：按天记，跨天清零 */
@TableName("t_task_progress")
public class TaskProgress implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** yyyy-MM-dd */
    @TableField("day")
    private String day;

    @TableField("task_id")
    private String taskId;

    @TableField("progress")
    private Integer progress;

    /** 0 未领奖 / 1 已领奖 */
    @TableField("claimed")
    private Integer claimed;

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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getClaimed() {
        return claimed;
    }

    public void setClaimed(Integer claimed) {
        this.claimed = claimed;
    }
}
