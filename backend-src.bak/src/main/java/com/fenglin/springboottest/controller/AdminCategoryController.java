package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PonyMapper;
import com.fenglin.springboottest.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 分类管理（一级=分类维度，二级=具体取值，自关联）。
 *   GET    /api/admin/category/list   扁平列表（按层级/排序）
 *   GET    /api/admin/category/tree   层级树（给小马表单做下拉用）
 *   POST   /api/admin/category        新增分类
 *   PUT    /api/admin/category        编辑分类
 *   DELETE /api/admin/category/{id}   删除分类（连带删掉它下面的子孙分类和所有条目关联）
 */
@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private PonyMapper ponyMapper;
    @Autowired
    private CategoryService categoryService;

    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request) {
        Result<?> a = auth(request);
        if (a != null) return a;
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getLevel)
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSortNo)
                .orderByAsc(Category::getId));
        return Result.ok(all);
    }

    @GetMapping("/tree")
    public Result<?> tree(HttpServletRequest request) {
        Result<?> a = auth(request);
        if (a != null) return a;
        return Result.ok(categoryService.tree());
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;

        Long parentId = toLong(body.get("parentId"));
        if (parentId == null || parentId == 0) parentId = 0L;

        Category c = new Category();
        c.setId(categoryMapper.nextCategoryId());
        c.setParentId(parentId);
        // 层级：一级分类 level=1；二级=父分类 level+1
        int level = 1;
        if (parentId != 0) {
            Category parent = categoryMapper.selectById(parentId);
            level = (parent == null || parent.getLevel() == null) ? 2 : parent.getLevel() + 1;
        }
        c.setLevel(level);
        c.setName(str(body.get("name")));
        c.setIcon(str(body.get("icon")));
        c.setColor(str(body.get("color")));
        c.setIntro(str(body.get("intro")));
        Integer sort = toInt(body.get("sortNo"));
        c.setSortNo(sort == null ? 0 : sort);
        if (c.getName() == null || c.getName().isEmpty()) return Result.fail("分类名不能为空");
        categoryMapper.insert(c);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        return Result.ok("已新增分类「" + c.getName() + "」", data);
    }

    @PutMapping
    public Result<?> update(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        Long id = toLong(body.get("id"));
        if (id == null) return Result.fail("缺少分类 id");
        Category c = categoryMapper.selectById(id);
        if (c == null) return Result.fail("找不到这个分类");

        if (body.containsKey("name")) c.setName(str(body.get("name")));
        if (body.containsKey("icon")) c.setIcon(str(body.get("icon")));
        if (body.containsKey("color")) c.setColor(str(body.get("color")));
        if (body.containsKey("intro")) c.setIntro(str(body.get("intro")));
        if (body.containsKey("parentId")) {
            Long parentId = toLong(body.get("parentId"));
            c.setParentId(parentId == null ? 0L : parentId);
        }
        if (body.containsKey("sortNo")) {
            Integer sort = toInt(body.get("sortNo"));
            c.setSortNo(sort == null ? 0 : sort);
        }
        categoryMapper.updateById(c);
        return Result.ok("已保存修改", null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable("id") Long id) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (id == null) return Result.fail("缺少分类 id");
        Category c = categoryMapper.selectById(id);
        if (c == null) return Result.fail("找不到这个分类");

        // 取「自己 + 全部子孙」，一并删掉，并清掉它们下面的条目关联
        List<Long> descendants = categoryMapper.selectDescendantIds(id);
        for (Long cid : descendants) {
            ponyMapper.deletePonyCategoryRelations(cid);
        }
        categoryMapper.deleteBatchIds(descendants);
        return Result.ok("已删除分类「" + c.getName() + "」及其下级", null);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
