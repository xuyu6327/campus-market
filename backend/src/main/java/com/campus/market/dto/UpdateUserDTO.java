package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 修改个人信息 DTO
 * 所有字段都是可选的，只更新提交的字段
 */
@Data
public class UpdateUserDTO {

    /** 昵称 */
    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** QQ号 */
    @Size(max = 20, message = "QQ号最多20个字符")
    private String qq;

    /** 微信号 */
    @Size(max = 50, message = "微信号最多50个字符")
    private String wechat;

    /** 学号 */
    @Size(max = 20, message = "学号最多20个字符")
    private String studentId;

    /** 真实姓名 */
    @Size(max = 50, message = "真实姓名最多50个字符")
    private String realName;
}
