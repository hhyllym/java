package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.AdminAuth;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.PonyTag;
import com.fenglin.springboottest.mapper.PonyTagMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 · 标签管理（t_pony_tag 里的小马关键词标签）。
 *   GET    /api/admin/tag/list   所有标签 + 各自出现次数
 *   POST   /api/admin/tag        给某只小马加一个标签
 *   PUT    /api/admin/tag/rename 批量把某标签改名（它下面所有小马一起改）
 *   DELETE /api/admin/tag/{tag}  删除某标签（所有小马上的这个标签都清掉）
 */
@RestController
@RequestMapping("/api/admin/tag")
public class AdminTagController {

    @Autowired
    private AdminAuth adminAuth;
    @Autowired
    private PonyTagMapper ponyTagMapper;

    private Result<?> auth(HttpServletRequest request) {
        return adminAuth.isValid(request.getHeader(AdminAuth.HEADER)) ? null : Result.fail("未授权或登录已过期");
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request) {
        Result<?> a = auth(request);
        if (a != null) return a;
        return Result.ok(ponyTagMapper.groupByTag());
    }

    /** 给某只小马加一个标签：{ ponyId, tag } */
    @PostMapping
    public Result<?> add(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        Long ponyId = toLong(body.get("ponyId"));
        String tag = body.get("tag") == null ? null : String.valueOf(body.get("tag")).trim();
        if (ponyId == null) return Result.fail("缺少 ponyId");
        if (tag == null || tag.isEmpty()) return Result.fail("标签内容不能为空");
        if (tag.length() > 24) return Result.fail("标签最长 24 字");

        long exist = ponyTagMapper.selectCount(new LambdaQueryWrapper<PonyTag>()
                .eq(PonyTag::getPonyId, ponyId).eq(PonyTag::getTag, tag));
        if (exist > 0) return Result.fail("这只小马已经有「" + tag + "」标签了");

        PonyTag pt = new PonyTag();
        pt.setPonyId(ponyId);
        pt.setTag(tag);
        ponyTagMapper.insert(pt);
        return Result.ok("已为小马添加标签「" + tag + "」", null);
    }

    @PutMapping("/rename")
    public Result<?> rename(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Result<?> a = auth(request);
        if (a != null) return a;
        String oldTag = body.get("oldTag") == null ? null : String.valueOf(body.get("oldTag")).trim();
        String newTag = body.get("newTag") == null ? null : String.valueOf(body.get("newTag")).trim();
        if (oldTag == null || oldTag.isEmpty() || newTag == null || newTag.isEmpty()) {
            return Result.fail("新旧标签名都要填");
        }
        int n = ponyTagMapper.renameTag(oldTag, newTag);
        return Result.ok("已将「" + oldTag + "」改名为「" + newTag + "」（影响 " + n + " 条）", null);
    }

    @DeleteMapping("/{tag}")
    public Result<?> delete(HttpServletRequest request, @PathVariable("tag") String tag) {
        Result<?> a = auth(request);
        if (a != null) return a;
        if (tag == null || tag.isEmpty()) return Result.fail("标签名不能为空");
        int n = ponyTagMapper.delete(new LambdaQueryWrapper<PonyTag>().eq(PonyTag::getTag, tag));
        return Result.ok("已删除标签「" + tag + "」（影响 " + n + " 条）", null);
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
