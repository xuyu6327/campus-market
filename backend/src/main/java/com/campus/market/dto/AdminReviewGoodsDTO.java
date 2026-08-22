package com.campus.market.dto;

import lombok.Data;

/**
 * 管理端商品重新上架审核 DTO
 * 对应接口：PUT /admin/goods/{id}/review
 */
@Data
public class AdminReviewGoodsDTO {

    /** 是否通过：true 通过上架 / false 驳回（保持下架） */
    private Boolean approve;

    /** 审核说明（驳回时必填，展示给卖家） */
    private String reason;
}
