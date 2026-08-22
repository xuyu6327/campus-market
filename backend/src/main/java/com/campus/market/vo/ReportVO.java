package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报 VO
 */
@Data
@Schema(description = "举报信息")
public class ReportVO {

    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "举报人ID")
    private Long reporterId;

    @Schema(description = "举报人昵称")
    private String reporterNickname;

    @Schema(description = "举报对象类型：1用户 2商品")
    private Integer targetType;

    @Schema(description = "举报对象类型描述")
    private String targetTypeDesc;

    @Schema(description = "举报对象ID")
    private Long targetId;

    @Schema(description = "举报对象名称（用户昵称或商品标题）")
    private String targetName;

    @Schema(description = "举报理由")
    private String reason;

    @Schema(description = "详细描述")
    private String description;

    @Schema(description = "处理状态：0待处理 1警告 2下架商品 3封禁账号 4驳回")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "处理人ID（管理员）")
    private Long handlerId;

    @Schema(description = "处理结果说明")
    private String handleResult;

    @Schema(description = "举报时间")
    private LocalDateTime createTime;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "当前用户是否是举报人")
    private Boolean isReporter;
}
