package com.campus.market.dto;

import lombok.Data;

/**
 * 管理端强制下架商品 DTO
 * 对应接口：PUT /admin/goods/{id}/takedown
 */
@Data
public class AdminTakedownGoodsDTO {

    /** 强制下架原因（展示给卖家，说明哪方面违规） */
    private String reason;
}
