package com.campus.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建举报 DTO
 */
@Data
@Schema(description = "创建举报请求")
public class CreateReportDTO {

    @Schema(description = "举报对象类型：1用户 2商品", example = "2")
    @NotNull(message = "举报对象类型不能为空")
    @Min(value = 1, message = "举报对象类型无效")
    @Max(value = 2, message = "举报对象类型无效")
    private Integer targetType;

    @Schema(description = "举报对象ID（用户ID或商品ID）", example = "1")
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @Schema(description = "举报理由（分类）", example = "违规商品")
    @NotBlank(message = "举报理由不能为空")
    @Size(max = 200, message = "举报理由不能超过200字")
    private String reason;

    @Schema(description = "详细描述（最多1000字）", example = "该商品描述与实际严重不符")
    @Size(max = 1000, message = "详细描述不能超过1000字")
    private String description;
}
