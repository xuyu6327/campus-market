package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理员处理举报DTO
 *
 * 处理动作：
 * 1=警告（通知被举报人）
 * 2=下架商品（仅当举报对象为商品时有效）
 * 3=封禁账号（仅当举报对象为用户时有效）
 * 4=驳回（通知举报人）
 */
@Data
public class AdminHandleReportDTO {

    /** 处理结果：1警告 2下架商品 3封禁账号 4驳回 */
    @NotNull(message = "处理结果不能为空")
    private Integer status;

    /** 处理结果说明 */
    @NotBlank(message = "处理说明不能为空")
    private String handleResult;

    /** 是否恶意举报（仅驳回时有效，标记后扣举报人信用分） */
    private Boolean isMalicious;
}
