package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价 VO
 */
@Data
@Schema(description = "评价信息")
public class ReviewVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品标题")
    private String goodsTitle;

    @Schema(description = "评价人角色：1买家评卖家 2卖家评买家")
    private Integer evaluatorRole;

    @Schema(description = "评价人昵称（匿名时脱敏）")
    private String evaluatorNickname;

    @Schema(description = "被评价人昵称（匿名时脱敏）")
    private String evaluateeNickname;

    @Schema(description = "评分（1-5星）")
    private Integer score;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "是否匿名：0实名 1匿名")
    private Integer isAnonymous;

    @Schema(description = "状态：0隐藏 1正常 2申诉中 3申诉后隐藏")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;

    @Schema(description = "当前用户是否是评价人")
    private Boolean isEvaluator;

    @Schema(description = "当前用户是否是被评价人")
    private Boolean isEvaluatee;
}
