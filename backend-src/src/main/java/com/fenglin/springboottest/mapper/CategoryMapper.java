package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 分类表相关查询。单表增删改查由 MyBatis-Plus 的 BaseMapper 提供。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 取「某个分类 + 它下面所有子孙分类」的 id（MySQL 8 递归 CTE）。
     * <p>
     * 这是分级分类搜索的核心：用户点一级分类「种族」时，
     * 要把「陆马 / 飞马 / 独角兽 / 天角兽」下挂的小马一起捞出来，
     * 而不是只查 category_id = 1（那样一只都查不到）。
     */
    @Select("WITH RECURSIVE `sub` AS ("
            + "  SELECT `id` FROM `t_category` WHERE `id` = #{id}"
            + "  UNION ALL"
            + "  SELECT c.`id` FROM `t_category` c JOIN `sub` s ON c.`parent_id` = s.`id`"
            + ") SELECT `id` FROM `sub`")
    List<Long> selectDescendantIds(@Param("id") Long id);

    /**
     * 全部「分类 ↔ 条目」关系（现在才 40 行，一次性捞进内存算数量最省事）。
     * 服务层据此自底向上求并集，得到每个分类（含子孙）下的小马数量——
     * 注意不能简单把子分类数量相加，因为紫悦同时属于「独角兽」和「天角兽」。
     */
    @Select("SELECT `pony_id` AS ponyId, `category_id` AS categoryId FROM `t_pony_category`")
    List<Map<String, Object>> selectAllRelations();
}
