package com.campus.market.vo;

import lombok.Data;

/**
 * 登录响应 VO
 * 返回 JWT token 和基本用户信息
 * 前端收到后存储 token，后续请求携带在 Authorization 头中
 */
@Data
public class LoginVO {

    /** JWT token */
    private String token;

    /** token 类型（固定 "Bearer"） */
    private String tokenType = "Bearer";

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色：0普通用户 1管理员 */
    private Integer role;

    /** 信用分 */
    private Integer creditScore;
}
