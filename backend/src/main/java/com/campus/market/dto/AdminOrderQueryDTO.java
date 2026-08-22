package com.campus.market.dto;

import lombok.Data;

/**
 * 后台订单查询DTO
 * 支持按订单编号/商品标题搜索，按状态筛选
 */
@Data
public class AdminOrderQueryDTO {

    /** 搜索关键词（订单编号/商品标题） */
    private String keyword;

    /** 订单状态：null全部 0待交易 1已完成 2买家取消 3卖家取消 4超时自动取消 */
    private Integer status;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
