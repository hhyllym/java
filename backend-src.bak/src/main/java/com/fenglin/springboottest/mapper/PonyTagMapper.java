package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.PonyTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 条目标签 t_pony_tag。
 * 单表增删改由 MyBatis-Plus 的 BaseMapper 提供；
 * 这里补两个后台管理需要的统计/批量方法。
 */
@Mapper
public interface PonyTagMapper extends BaseMapper<PonyTag> {

    /** 所有出现过的标签 + 各自出现次数（标签管理页用） */
    @Select("SELECT `tag` AS tag, COUNT(*) AS cnt FROM `t_pony_tag` GROUP BY `tag` ORDER BY cnt DESC, `tag`")
    List<Map<String, Object>> groupByTag();

    /** 批量改标签名（标签重命名时，把它下面所有小马都改掉） */
    @Update("UPDATE `t_pony_tag` SET `tag` = #{newTag} WHERE `tag` = #{oldTag}")
    int renameTag(@Param("oldTag") String oldTag, @Param("newTag") String newTag);
}
