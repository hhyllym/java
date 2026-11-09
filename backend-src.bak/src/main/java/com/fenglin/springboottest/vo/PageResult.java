package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页结果：{ list, total, page, size, pages, hasMore }
 */
public class PageResult<T> implements Serializable {

    private List<T> list = new ArrayList<>();
    private long total;
    private int page = 1;
    private int size = 12;
    private int pages;
    private boolean hasMore;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list == null ? new ArrayList<>() : list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
        this.hasMore = (long) page * size < total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
