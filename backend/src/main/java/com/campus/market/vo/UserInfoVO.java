package com.campus.market.vo;

import lombok.Data;

/**
 * 用户个人信息 VO
 * 返回给前端的用户信息（脱敏后的手机号）
 */
@Data
public class UserInfoVO {

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号（脱敏显示，如 138****8888） */
    private String phone;

    /** 学号 */
    private String studentId;

    /** 真实姓名 */
    private String realName;

    /** QQ号 */
    private String qq;

    /** 微信号 */
    private String wechat;

    /** 信用分 */
    private Integer creditScore;

    /** 角色：0普通用户 1管理员 */
    private Integer role;

    /** 账号状态：0禁用 1正常 */
    private Integer status;

    /** 注册时间 */
    private String createTime;
}
