package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Pony;
import com.fenglin.springboottest.entity.PonyTag;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.PonyMapper;
import com.fenglin.springboottest.mapper.PonyTagMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 小马图鉴管理（对应首页主展示区）。
 *   GET  /api/admin/pony/list          分页列表（含标签、所属分类）
 *   POST /api/admin/pony               新增一只小马（含标签、分类）
 *   PUT  /api/admin/pony               编辑一只小马（含标签、分类）
 *   DELETE /api/admin/pony/{id}        删除一只小马（连带清掉它的标签/分类关联）
 * 所有接口都要带 X-Admin-Token 请求头，由 AdminAuth 校验。
 */
@RestController
@RequestMapping("/api/admin/pony")
public class AdminPonyController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private PonyMapper ponyMapper;
    @Autowired
    private PonyTagMapper ponyTagMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    /** 校验后台令牌，不通过直接返回错误结果（调用方判断非空就直接 return） */
    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "12") int size) {
        Result<?> a = auth(request);
        if (a != null) return a;

        String kw = keyword == null ? "" : keyword.trim();
        page = Math.max(1, page);
        size = Math.max(1, Math.min(50, size));

        LambdaQueryWrapper<Pony> countW = new LambdaQueryWrapper<>();
        if (!kw.isEmpty()) {
            countW.like(Pony::getName, kw).or().like(Pony::getAlias, kw).or().like(Pony::getIntro, kw);
        }
        long total = ponyMapper.selectCount(countW);

        LambdaQueryWrapper<Pony> listW = new LambdaQueryWrapper<>();
        if (!kw.isEmpty()) {
            listW.like(Pony::getName, kw).or().like(Pony::getAlias, kw).or().like(Pony::getIntro, kw);
        }
        listW.orderByAsc(Pony::getSortNo).orderByAsc(Pony::getId)
                .last("LIMIT " + ((page - 1) * size) + ", " + size);
        List<Pony> rows = ponyMapper.selectList(listW);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", toVOList(rows));
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("pages", (total + size - 1) / Math.max(1, size));
        return Result.ok(data);
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;

        Long id = ponyMapper.nextPonyId();
        Pony pony = new Pony();
        pony.setId(id);
        fillPony(pony, body, false);
        pony.setViewCount(0);
        ponyMapper.insert(pony);
        saveRelations(id, body);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        return Result.ok("已新增小马「" + pony.getName() + "」", data);
    }

    @PutMapping
    public Result<?> update(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;

        Long id = toLong(body.get("id"));
        if (id == null) return Result.fail("缺少小马 id");
        Pony exist = ponyMapper.selectById(id);
        if (exist == null) return Result.fail("找不到这只小马");

        fillPony(exist, body, true);
        ponyMapper.updateById(exist);
        // 标签/分类：先清后写（覆盖式保存）
        saveRelations(id, body);
        return Result.ok("已保存修改", null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (id == null) return Result.fail("缺少小马 id");
        Pony exist = ponyMapper.selectById(id);
        if (exist == null) return Result.fail("找不到这只小马");

        ponyTagMapper.delete(new LambdaQueryWrapper<PonyTag>().eq(PonyTag::getPonyId, id));
        ponyMapper.deletePonyCategories(id);
        ponyMapper.deleteById(id);
        return Result.ok("已删除「" + exist.getName() + "」", null);
    }

    // ------------------------- 内部辅助 -------------------------

    /** 把请求体里的字段写进 Pony 实体（create 时全量，update 时只改允许改的） */
    private void fillPony(Pony pony, Map<String, Object> body, boolean isUpdate) {
        String name = str(body.get("name"));
        if (name != null) pony.setName(name);
        if (body.containsKey("alias")) pony.setAlias(str(body.get("alias")));
        if (body.containsKey("mark")) pony.setMark(str(body.get("mark")));
        if (body.containsKey("emoji")) pony.setEmoji(str(body.get("emoji")));
        if (body.containsKey("color")) pony.setColor(str(body.get("color")));
        if (body.containsKey("rarity")) pony.setRarity(str(body.get("rarity")));
        if (body.containsKey("intro")) pony.setIntro(str(body.get("intro")));
        if (body.containsKey("sortNo")) {
            Integer s = toInt(body.get("sortNo"));
            pony.setSortNo(s == null ? 0 : s);
        }
    }

    /** 保存标签 + 分类关联（先清后写） */
    private void saveRelations(Long ponyId, Map<String, Object> body) {
        ponyTagMapper.delete(new LambdaQueryWrapper<PonyTag>().eq(PonyTag::getPonyId, ponyId));
        ponyMapper.deletePonyCategories(ponyId);

        for (String tag : toStringList(body.get("tags"))) {
            if (tag == null || tag.trim().isEmpty()) continue;
            PonyTag pt = new PonyTag();
            pt.setPonyId(ponyId);
            pt.setTag(tag.trim());
            ponyTagMapper.insert(pt);
        }
        for (Long cid : toLongList(body.get("categoryIds"))) {
            if (cid == null) continue;
            ponyMapper.addPonyCategory(ponyId, cid);
        }
    }

    /** 复用首页那套「Pony → 带标签/分类的 VO」逻辑 */
    private List<Map<String, Object>> toVOList(List<Pony> ponies) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (ponies == null || ponies.isEmpty()) return result;

        List<Long> ids = new ArrayList<>();
        for (Pony p : ponies) ids.add(p.getId());

        Map<Long, List<String>> tagMap = new HashMap<>();
        for (Map<String, Object> row : ponyMapper.selectTagsByPonyIds(ids)) {
            Long pid = toLong(row.get("ponyId"));
            if (pid != null) tagMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(String.valueOf(row.get("tag")));
        }
        Map<Long, List<Map<String, Object>>> catMap = new HashMap<>();
        for (Map<String, Object> row : ponyMapper.selectCategoriesByPonyIds(ids)) {
            Long pid = toLong(row.get("ponyId"));
            if (pid != null) catMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(row);
        }

        for (Pony p : ponies) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", p.getId());
            vo.put("name", p.getName());
            vo.put("alias", p.getAlias());
            vo.put("mark", p.getMark());
            vo.put("emoji", p.getEmoji());
            vo.put("color", p.getColor());
            vo.put("rarity", p.getRarity());
            vo.put("intro", p.getIntro());
            vo.put("viewCount", p.getViewCount() == null ? 0 : p.getViewCount());
            vo.put("sortNo", p.getSortNo());
            vo.put("tags", tagMap.getOrDefault(p.getId(), new ArrayList<>()));
            vo.put("categories", catMap.getOrDefault(p.getId(), new ArrayList<>()));
            result.add(vo);
        }
        return result;
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

    private List<String> toStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (x != null) out.add(String.valueOf(x));
            }
        } else if (o != null) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    private List<Long> toLongList(Object o) {
        List<Long> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                Long v = toLong(x);
                if (v != null) out.add(v);
            }
        } else {
            Long v = toLong(o);
            if (v != null) out.add(v);
        }
        return out;
    }
}
