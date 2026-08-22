package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台用户管理VO
 * 管理员可见完整用户信息（含解密手机号）
 */
@Data
public class AdminUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号（管理员可见明文） */
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

    /** 角色描述 */
    private String roleDesc;

    /** 账号状态：0禁用 1正常 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 注销状态：0正常 1待注销 */
    private Integer cancelStatus;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
