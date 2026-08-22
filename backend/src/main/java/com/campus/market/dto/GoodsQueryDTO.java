package com.campus.market.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表查询 DTO
 * 对应接口：GET /goods/list
 *
 * 支持的筛选条件：
 * - 分类筛选（categoryId）
 * - 价格区间（minPrice / maxPrice）
 * - 成色筛选（goodsCondition）
 * - 关键词搜索（keyword）
 *
 * 支持的排序方式：
 * - newest: 按发布时间倒序（默认）
 * - price_asc: 价格升序
 * - price_desc: 价格降序
 * - popular: 按浏览量倒序
 */
@Data
public class GoodsQueryDTO {

    /** 页码（默认1） */
    private Integer pageNum = 1;

    /** 每页数量（默认10） */
    private Integer pageSize = 10;

    /** 分类ID（可选） */
    private Long categoryId;

    /** 关键词（可选，搜索标题） */
    private String keyword;

    /** 最低价格（可选） */
    private BigDecimal minPrice;

    /** 最高价格（可选） */
    private BigDecimal maxPrice;

    /** 成色筛选（可选） */
    private Integer goodsCondition;

    /** 排序方式：newest / price_asc / price_desc / popular */
    private String sortBy = "newest";

    /** 状态筛选（默认只看在售） */
    private Integer status = 1;
}
