package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.vo.CategoryNode;

import java.util.List;

/**
 * 图鉴分类（分级）。
 */
public interface CategoryService {

    /** 全部分类，按 level / 父分类 / sort_no 排好 */
    List<Category> listAll();

    /** 分类树（一级 → 二级…），每个节点的 count 是该分类含子孙分类下的小马总数 */
    List<CategoryNode> tree();

    /** 单个分类，查不到返回 null */
    Category get(Long id);

    /**
     * 某个分类及其全部子孙分类的 id。
     * categoryId 为 null 时返回空集合（表示「不限分类」）。
     */
    List<Long> descendantIds(Long categoryId);

    /** 从一级分类到自己的名字链，如 ["种族", "飞马"]；查不到返回空集合 */
    List<String> pathNames(Long categoryId);
}
