package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Pony;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 小马图鉴条目。动态搜索 SQL 写在 resources/mapper/PonyMapper.xml 里。
 */
@Mapper
public interface PonyMapper extends BaseMapper<Pony> {

    /**
     * 分级分类 + 关键词搜索（分页）。
     *
     * @param keyword     关键词，可为空；匹配 名字 / 别名 / 简介 / 标签
     * @param categoryIds 分类 id 集合（已含子孙分类），可为空；为空表示全部分类
     * @param offset      起始行
     * @param size        每页条数
     */
    List<Pony> search(@Param("keyword") String keyword,
                      @Param("categoryIds") List<Long> categoryIds,
                      @Param("offset") int offset,
                      @Param("size") int size);

    /** 上面那个搜索的总条数（分页用） */
    long countSearch(@Param("keyword") String keyword,
                     @Param("categoryIds") List<Long> categoryIds);

    /** 批量取标签：返回 [{ponyId, tag}, ...] */
    List<Map<String, Object>> selectTagsByPonyIds(@Param("ponyIds") List<Long> ponyIds);

    /**
     * 批量取条目所属分类（含父分类名，前端拿来拼「🦄 种族 · 飞马」）。
     * 返回 [{ponyId, id, name, icon, color, level, parentId, parentName, parentIcon}, ...]
     */
    List<Map<String, Object>> selectCategoriesByPonyIds(@Param("ponyIds") List<Long> ponyIds);

    /** 看详情时热度 +1 */
    @Update("UPDATE `t_pony` SET `view_count` = `view_count` + 1 WHERE `id` = #{id}")
    int increaseView(@Param("id") Long id);
}
