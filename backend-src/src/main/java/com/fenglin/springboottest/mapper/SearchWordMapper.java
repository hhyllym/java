package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SearchWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SearchWordMapper extends BaseMapper<SearchWord> {

    /**
     * 热搜词累加：这个词没出现过就插一条（次数 1），出现过就 +1。
     * 用一条 SQL 解决，避免「先查再插」的并发重复插入。
     */
    @Update("INSERT INTO `t_search_word` (`word`, `hit_count`) VALUES (#{word}, 1) "
            + "ON DUPLICATE KEY UPDATE `hit_count` = `hit_count` + 1")
    int increaseHit(@Param("word") String word);

    /** 首页热搜词：人工权重大的先，其次按搜索次数 */
    @Select("SELECT * FROM `t_search_word` "
            + "ORDER BY `sort_no` DESC, `hit_count` DESC, `id` ASC LIMIT #{limit}")
    List<SearchWord> selectHot(@Param("limit") int limit);

    /** 所有词条的搜索次数之和，首页统计用 */
    @Select("SELECT IFNULL(SUM(`hit_count`), 0) FROM `t_search_word`")
    long sumHits();
}
