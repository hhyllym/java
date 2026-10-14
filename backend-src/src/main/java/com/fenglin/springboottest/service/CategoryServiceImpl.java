package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.vo.CategoryNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getLevel)
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSortNo)
                .orderByAsc(Category::getId));
    }

    @Override
    public Category get(Long id) {
        return id == null ? null : categoryMapper.selectById(id);
    }

    @Override
    public List<Long> descendantIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        return categoryMapper.selectDescendantIds(categoryId);
    }

    @Override
    public List<String> pathNames(Long categoryId) {
        List<String> path = new LinkedList<>();
        Category cur = get(categoryId);
        int guard = 0;
        while (cur != null && guard++ < 10) {          // guard 防止脏数据成环死循环
            path.add(0, cur.getName());
            Long pid = cur.getParentId();
            cur = (pid == null || pid == 0L) ? null : get(pid);
        }
        return path;
    }

    @Override
    public List<CategoryNode> tree() {
        List<Category> all = listAll();

        // 1) 分类 → 直接挂在它下面（不含子孙）的小马 id 集合
        Map<Long, Set<Long>> own = new HashMap<>();
        for (Map<String, Object> row : categoryMapper.selectAllRelations()) {
            Long cid = toLong(row.get("categoryId"));
            Long pid = toLong(row.get("ponyId"));
            if (cid == null || pid == null) {
                continue;
            }
            own.computeIfAbsent(cid, k -> new HashSet<>()).add(pid);
        }

        // 2) 建节点
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        for (Category c : all) {
            CategoryNode n = new CategoryNode();
            n.setId(c.getId());
            n.setParentId(c.getParentId());
            n.setName(c.getName());
            n.setLevel(c.getLevel());
            n.setIcon(c.getIcon());
            n.setColor(c.getColor());
            n.setIntro(c.getIntro());
            n.setCount(0);
            nodes.put(c.getId(), n);
        }

        // 3) 挂父子关系
        List<CategoryNode> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryNode node = nodes.get(c.getId());
            Long pid = c.getParentId();
            if (pid == null || pid == 0L) {
                roots.add(node);
            } else {
                CategoryNode parent = nodes.get(pid);
                if (parent == null) {
                    roots.add(node);          // 父分类不存在时当一级分类显示，避免整棵树丢节点
                } else {
                    parent.getChildren().add(node);
                }
            }
        }

        // 4) 自底向上求并集，算出「含子孙分类」的小马数量
        for (CategoryNode root : roots) {
            collect(root, own);
        }
        return roots;
    }

    /** 返回该节点子树里全部小马 id（去重），并把大小写进 node.count */
    private Set<Long> collect(CategoryNode node, Map<Long, Set<Long>> own) {
        Set<Long> union = new HashSet<>(own.getOrDefault(node.getId(), Collections.emptySet()));
        for (CategoryNode child : node.getChildren()) {
            union.addAll(collect(child, own));
        }
        node.setCount(union.size());
        return union;
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
