package com.campus.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 预订商品 DTO（买家下单）
 * 买家可选填手机/微信/QQ 联系方式（至少填一种，仅该订单卖家可见）
 * 手机号后端加密快照，QQ/微信明文快照
 */
@Data
@Schema(description = "预订商品请求")
public class CreateOrderDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1")
    private Long goodsId;

    @Schema(description = "买家手机号（选填，填了卖家才可见）", example = "13812345678")
    private String buyerPhone;

    @Schema(description = "买家QQ号（选填，填了卖家才可见）", example = "123456789")
    private String buyerQq;

    @Schema(description = "买家微信号（选填，填了卖家才可见）", example = "wx_123456")
    private String buyerWechat;
}
