package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Pony;
import com.fenglin.springboottest.entity.SearchWord;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PonyMapper;
import com.fenglin.springboottest.mapper.PonyTagMapper;
import com.fenglin.springboottest.mapper.SearchWordMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.vo.PageResult;
import com.fenglin.springboottest.vo.PonyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PonyServiceImpl implements PonyService {

    /** 首页热门小马展示几只 */
    private static final int RECOMMEND_SIZE = 6;
    /** 详情页「同类推荐」最多几只 */
    private static final int RELATED_MAX = 4;

    @Autowired
    private PonyMapper ponyMapper;

    @Autowired
    private PonyTagMapper ponyTagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SearchWordMapper searchWordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryService categoryService;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // ---------- 1) 统计数字 ----------
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("ponyTotal", ponyMapper.selectCount(null));
        stats.put("categoryTotal", categoryMapper.selectCount(null));
        stats.put("tagTotal", ponyTagMapper.selectCount(null));
        stats.put("memberTotal", userMapper.selectCount(null));
        stats.put("searchTimes", searchWordMapper.sumHits());
        data.put("stats", stats);

        // ---------- 2) 分级分类树（带每类数量） ----------
        data.put("categoryTree", categoryService.tree());

        // ---------- 3) 热搜词（搜索过就会自己长） ----------
        List<Map<String, Object>> hotWords = new ArrayList<>();
        for (SearchWord w : searchWordMapper.selectHot(8)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("word", w.getWord());
            item.put("hitCount", w.getHitCount());
            hotWords.add(item);
        }
        data.put("hotWords", hotWords);

        // ---------- 4) 热门小马（按被查看次数） ----------
        List<Pony> hot = ponyMapper.selectList(new LambdaQueryWrapper<Pony>()
                .orderByDesc(Pony::getViewCount)
                .orderByAsc(Pony::getSortNo)
                .orderByAsc(Pony::getId)
                .last("limit " + RECOMMEND_SIZE));
        data.put("recommends", toVOList(hot));

        return data;
    }

    @Override
    public Map<String, Object> search(String keyword, Long categoryId, int page, int size) {
        String kw = keyword == null ? "" : keyword.trim();
        page = Math.max(1, page);
        size = Math.max(1, Math.min(50, size));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("keyword", kw);
        data.put("page", page);
        data.put("size", size);

        // ---------- 1) 解析分类：一级分类要展开成「自己 + 全部子孙」 ----------
        List<Long> categoryIds = null;
        if (categoryId != null) {
            Category category = categoryService.get(categoryId);
            if (category == null) {
                // 分类不存在，直接返回空，不能当成「不限分类」否则会错误地返回全部数据
                data.put("category", null);
                data.put("list", new ArrayList<>());
                data.put("total", 0L);
                data.put("pages", 0);
                data.put("hasMore", false);
                data.put("tip", "没有找到这个分类");
                return data;
            }
            categoryIds = categoryService.descendantIds(categoryId);

            List<String> path = categoryService.pathNames(categoryId);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", category.getId());
            info.put("name", category.getName());
            info.put("level", category.getLevel());
            info.put("parentId", category.getParentId());
            info.put("icon", category.getIcon());
            info.put("color", category.getColor());
            info.put("pathNames", path);
            info.put("path", String.join(" › ", path));
            data.put("category", info);
        } else {
            data.put("category", null);
        }

        // ---------- 2) 有关键词就累加热搜次数 ----------
        if (!kw.isEmpty()) {
            searchWordMapper.increaseHit(kw);
        }

        // ---------- 3) 分页查询 ----------
        long total = ponyMapper.countSearch(kw, categoryIds);
        List<Pony> rows = total == 0
                ? new ArrayList<>()
                : ponyMapper.search(kw, categoryIds, (page - 1) * size, size);
        PageResult<PonyVO> pr = new PageResult<>(toVOList(rows), total, page, size);

        data.put("list", pr.getList());
        data.put("total", pr.getTotal());
        data.put("pages", pr.getPages());
        data.put("hasMore", pr.isHasMore());
        return data;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Pony pony = id == null ? null : ponyMapper.selectById(id);
        if (pony == null) {
            return null;
        }

        ponyMapper.increaseView(id);
        pony.setViewCount((pony.getViewCount() == null ? 0 : pony.getViewCount()) + 1);

        PonyVO vo = toVOList(Collections.singletonList(pony)).get(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pony", vo);

        // 同类推荐：拿这只小马所属的二级分类，找同一批分类下的其他小马
        List<Long> leafIds = new ArrayList<>();
        for (Map<String, Object> c : vo.getCategories()) {
            Object level = c.get("level");
            Object cid = c.get("id");
            if (level != null && cid != null && toInt(level) >= 2) {
                Long l = toLong(cid);
                if (l != null) {
                    leafIds.add(l);
                }
            }
        }

        List<PonyVO> related = new ArrayList<>();
        if (!leafIds.isEmpty()) {
            for (PonyVO other : toVOList(ponyMapper.search("", leafIds, 0, RELATED_MAX + 3))) {
                if (!other.getId().equals(id)) {
                    related.add(other);
                }
                if (related.size() >= RELATED_MAX) {
                    break;
                }
            }
        }
        data.put("related", related);
        return data;
    }

    @Override
    public PonyVO findByName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            return null;
        }
        // 先按中文名精确匹配，再按英文名（别名）匹配，都不区分大小写
        List<Pony> rows = ponyMapper.selectList(new LambdaQueryWrapper<Pony>()
                .and(w -> w.eq(Pony::getName, n)
                        .or()
                        .apply("LOWER(`alias`) = {0}", n.toLowerCase()))
                .orderByAsc(Pony::getSortNo)
                .orderByAsc(Pony::getId)
                .last("limit 1"));
        return rows.isEmpty() ? null : toVOList(rows).get(0);
    }

    /**
     * Pony → PonyVO，并批量补齐标签和所属分类（避免 N+1 查询）。
     */
    private List<PonyVO> toVOList(List<Pony> ponies) {
        List<PonyVO> result = new ArrayList<>();
        if (ponies == null || ponies.isEmpty()) {
            return result;
        }

        List<Long> ids = new ArrayList<>();
        for (Pony p : ponies) {
            ids.add(p.getId());
        }

        Map<Long, List<String>> tagMap = new HashMap<>();
        for (Map<String, Object> row : ponyMapper.selectTagsByPonyIds(ids)) {
            Long pid = toLong(row.get("ponyId"));
            if (pid != null) {
                tagMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(String.valueOf(row.get("tag")));
            }
        }

        Map<Long, List<Map<String, Object>>> categoryMap = new HashMap<>();
        for (Map<String, Object> row : ponyMapper.selectCategoriesByPonyIds(ids)) {
            Long pid = toLong(row.get("ponyId"));
            if (pid != null) {
                categoryMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(row);
            }
        }

        for (Pony p : ponies) {
            PonyVO vo = new PonyVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setAlias(p.getAlias());
            vo.setMark(p.getMark());
            vo.setEmoji(p.getEmoji());
            vo.setColor(p.getColor());
            vo.setRarity(p.getRarity());
            vo.setIntro(p.getIntro());
            vo.setViewCount(p.getViewCount() == null ? 0 : p.getViewCount());
            vo.setTags(tagMap.getOrDefault(p.getId(), new ArrayList<>()));
            vo.setCategories(categoryMap.getOrDefault(p.getId(), new ArrayList<>()));
            result.add(vo);
        }
        return result;
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

    private int toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }
}
