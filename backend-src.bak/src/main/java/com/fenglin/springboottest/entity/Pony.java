package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小马图鉴条目 t_pony。
 * mark 是前端 CutieMark.vue 支持的手绘标志 key（pinkie/twilight/dash/applejack/fluttershy/rarity），
 * 没有对应图案的填 star，前端会退回用 emoji 显示。
 */
@TableName("t_pony")
public class Pony implements Serializable {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("alias")
    private String alias;

    @TableField("mark")
    private String mark;

    @TableField("emoji")
    private String emoji;

    @TableField("color")
    private String color;

    @TableField("rarity")
    private String rarity;

    @TableField("intro")
    private String intro;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("sort_no")
    private Integer sortNo;

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
