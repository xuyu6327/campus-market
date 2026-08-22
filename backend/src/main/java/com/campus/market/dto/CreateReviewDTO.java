package com.campus.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建评价 DTO
 */
@Data
@Schema(description = "创建评价请求")
public class CreateReviewDTO {

    @Schema(description = "订单ID", example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "评分（1-5星）", example = "5")
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer score;

    @Schema(description = "评价内容（最多500字）", example = "卖家态度很好，商品成色如实描述")
    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    @Schema(description = "是否匿名：0实名 1匿名", example = "1")
    private Integer isAnonymous = 1;
}
