package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户公开主页信息 VO
 * 用于查看他人主页，只暴露公开信息
 */
@Data
public class UserPublicVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 信用分 */
    private Integer creditScore;

    /** 账号状态：0正常 1冻结 */
    private Integer status;

    /** 在售商品数 */
    private Long onSaleCount;

    /** 好评率（百分比，0-100） */
    private Integer goodRate;

    /** 注册时间 */
    private String createTime;
}
