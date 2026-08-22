package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 VO（列表 + 详情共用）
 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品标题")
    private String goodsTitle;

    @Schema(description = "商品封面图")
    private String goodsCoverImage;

    @Schema(description = "商品价格")
    private BigDecimal goodsPrice;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "买家昵称")
    private String buyerNickname;

    @Schema(description = "买家手机号（解密明文，买家填了才展示，仅卖家可查看）")
    private String buyerPhone;

    @Schema(description = "买家QQ号（买家填了才展示，仅卖家可查看）")
    private String buyerQq;

    @Schema(description = "买家微信号（买家填了才展示，仅卖家可查看）")
    private String buyerWechat;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "卖家昵称")
    private String sellerNickname;

    @Schema(description = "卖家账号状态：0正常 1禁用（冻结，用于展示提示）")
    private Integer sellerStatus;

    @Schema(description = "卖家QQ")
    private String sellerQq;

    @Schema(description = "卖家微信")
    private String sellerWechat;

    @Schema(description = "卖家手机号（解密，仅买家可查看，卖家留了手机号时才有）")
    private String sellerPhone;

    @Schema(description = "订单状态：0待交易 1已完成 2买家取消 3卖家取消 4超时自动取消")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "当前用户是否为买家")
    private Boolean isBuyer;

    @Schema(description = "当前用户是否为卖家")
    private Boolean isSeller;

    @Schema(description = "是否已提交'联系不上卖家'")
    private Boolean contactFailed;

    @Schema(description = "联系不上卖家提交时间")
    private LocalDateTime contactFailAt;

    @Schema(description = "实际交易完成时间")
    private LocalDateTime tradeTime;

    @Schema(description = "当前用户是否已评价该订单")
    private Boolean rated;

    @Schema(description = "预订时间")
    private LocalDateTime createTime;
}
