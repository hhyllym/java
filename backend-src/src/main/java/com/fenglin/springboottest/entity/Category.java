package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 图鉴分类表 t_category，自关联（parent_id = 0 表示一级分类）。
 * 现在是「一级=分类维度（种族/元素/栖息地）」「二级=具体取值（飞马/忠诚/小马谷）」，
 * 但表结构本身支持任意层级，后端取子孙分类用的是递归 CTE。
 */
@TableName("t_category")
public class Category implements Serializable {

    /** 分类 id 由 SQL 脚本指定，不是自增 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("level")
    private Integer level;

    @TableField("icon")
    private String icon;

    @TableField("color")
    private String color;

    @TableField("intro")
    private String intro;

    @TableField("sort_no")
    private Integer sortNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
