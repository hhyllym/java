package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.PonyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 条目详情：GET /api/pony/{id}
 * 返回 { pony: {...}, related: [...] }，看一次 view_count 就 +1。
 */
@RestController
@RequestMapping("/api/pony")
public class PonyController {

    @Autowired
    private PonyService ponyService;

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable("id") Long id) {
        Map<String, Object> data = ponyService.detail(id);
        if (data == null) {
            return Result.fail("没有找到这只小马");
        }
        return Result.ok(data);
    }
}
