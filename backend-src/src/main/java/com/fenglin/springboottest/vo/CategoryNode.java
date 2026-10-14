package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点。count = 该分类及其所有子孙分类下的小马总数，
 * 所以一级分类「种族」的 count 是 16，二级分类「飞马」的 count 是 3。
 */
public class CategoryNode implements Serializable {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private String icon;
    private String color;
    private String intro;
    /** 该分类（含子孙分类）下的小马数量 */
    private Integer count;
    private List<CategoryNode> children = new ArrayList<>();

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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<CategoryNode> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryNode> children) {
        this.children = children;
    }
}
