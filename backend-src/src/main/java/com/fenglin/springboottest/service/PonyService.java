package com.fenglin.springboottest.service;

import java.util.Map;

import com.fenglin.springboottest.vo.PonyVO;

/**
 * 首页图鉴：展示 + 分级分类搜索。
 */
public interface PonyService {

    /** 首页展示用的一次性数据：统计 / 分类树 / 热搜词 / 热门小马 */
    Map<String, Object> overview();

    /**
     * 分级分类 + 关键词搜索。
     *
     * @param keyword    关键词，可为空（只按分类筛）
     * @param categoryId 分类 id，可以为一级或二级；为空表示全部分类
     * @param page       从 1 开始
     * @param size       每页条数，内部限制 1~50
     */
    Map<String, Object> search(String keyword, Long categoryId, int page, int size);

    /** 条目详情（含所属分类、标签、同类推荐）；查不到返回 null */
    Map<String, Object> detail(Long id);

    /**
     * 按名字或英文名精确找一只小马（「我的账号」页拿它做“同名小马”），查不到返回 null。
     */
    PonyVO findByName(String name);
}
