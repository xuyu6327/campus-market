package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求 DTO
 * 前端提交：手机号 + 密码 + 昵称
 */
@Data
public class RegisterDTO {

    /** 手机号（11位） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 密码（6-20位） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    /** 学号（选填） */
    @Size(max = 20, message = "学号最多20个字符")
    private String studentId;

    /** 真实姓名（选填） */
    @Size(max = 50, message = "真实姓名最多50个字符")
    private String realName;
}
