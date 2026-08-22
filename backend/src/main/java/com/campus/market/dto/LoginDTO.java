package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 * 支持两种登录方式：
 * 1. 手机号 + 密码
 * 2. 学号 + 密码
 * 前端提交 account 字段，后端自动判断是手机号还是学号
 */
@Data
public class LoginDTO {

    /** 账号（手机号或学号） */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
