package com.campus.market.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数工具类
 * 统一限制 pageSize 上限，防止恶意传超大值拖垮数据库
 */
public class PageUtil {

    /** 单页最大条数 */
    private static final long MAX_PAGE_SIZE = 50;

    /** 默认每页条数 */
    private static final long DEFAULT_PAGE_SIZE = 10;

    private PageUtil() {
    }

    /**
     * 规范化 pageSize：null 或 <=0 时用默认值，超过上限时用上限
     */
    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return (int) DEFAULT_PAGE_SIZE;
        }
        return (int) Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 规范化 pageNum：null 或 <=0 时用 1
     */
    public static int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 创建规范化的分页对象
     */
    public static <T> Page<T> of(Integer pageNum, Integer pageSize) {
        return new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
    }
}
