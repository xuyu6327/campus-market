package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理员调整用户信用分DTO
 */
@Data
public class AdminAdjustCreditDTO {

    /** 变更分值（正数加分，负数扣分） */
    @NotNull(message = "变更分值不能为空")
    private Integer changeValue;

    /** 变更原因 */
    @NotBlank(message = "变更原因不能为空")
    private String reason;
}
