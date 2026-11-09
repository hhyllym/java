package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.PonyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页接口，前端 src/home.js 调这两个：
 *   GET /api/home/overview  首页展示：统计 + 分类树 + 热搜词 + 热门小马
 *   GET /api/home/search    分级分类 + 关键词搜索（分页）
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PonyService ponyService;

    @GetMapping("/overview")
    public Result<?> overview() {
        return Result.ok(ponyService.overview());
    }

    /**
     * @param keyword    关键词（匹配名字/别名/简介/标签），可不传
     * @param categoryId 分类 id（一级或二级都行），不传表示全部分类
     */
    @GetMapping("/search")
    public Result<?> search(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "categoryId", required = false) Long categoryId,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "12") int size) {
        log.info("首页搜索 keyword={} categoryId={} page={} size={}", keyword, categoryId, page, size);
        return Result.ok(ponyService.search(keyword, categoryId, page, size));
    }
}
