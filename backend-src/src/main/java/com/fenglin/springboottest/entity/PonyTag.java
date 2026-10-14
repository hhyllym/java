package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 条目标签 t_pony_tag：搜索时「碧琪」能搜到「派对」，命中的就是这张表。
 */
@TableName("t_pony_tag")
public class PonyTag implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("pony_id")
    private Long ponyId;

    @TableField("tag")
    private String tag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPonyId() {
        return ponyId;
    }

    public void setPonyId(Long ponyId) {
        this.ponyId = ponyId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
