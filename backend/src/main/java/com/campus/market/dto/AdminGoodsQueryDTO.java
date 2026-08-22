package com.campus.market.dto;

import lombok.Data;

/**
 * 后台商品查询DTO
 * 支持按标题模糊搜索，按状态和分类筛选
 */
@Data
public class AdminGoodsQueryDTO {

    /** 搜索关键词（商品标题） */
    private String keyword;

    /** 商品状态：null全部 0下架 1在售 2预订中 3已售出 */
    private Integer status;

    /** 分类ID */
    private Long categoryId;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
