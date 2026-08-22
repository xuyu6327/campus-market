package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 VO
 * 用于商品详情页展示
 * 包含完整信息：商品信息 + 卖家信息 + 联系方式 + 当前用户收藏状态
 */
@Data
public class GoodsDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色：1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5严重使用痕迹 */
    private Integer goodsCondition;

    /** 成色描述 */
    private String conditionDesc;

    /** 图片URL列表 */
    private List<String> images;

    /** 交易地点 */
    private String tradeLocation;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 状态：0下架 1在售 2预订中 3已售出 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer favoriteCount;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 最近上架时间 */
    private LocalDateTime lastRelistedAt;

    // ================== 卖家信息 ==================

    /** 卖家用户ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 卖家头像 */
    private String sellerAvatar;

    /** 卖家信用分 */
    private Integer sellerCreditScore;

    /** 卖家账号状态：0正常 1禁用（冻结，用于展示"卖家已被冻结"提示） */
    private Integer sellerStatus;

    // ================== 联系方式 ==================

    /** 联系方式类型：1手机 2QQ 3微信 */
    private Integer contactMethod;

    /** 联系方式描述 */
    private String contactMethodDesc;

    /** 联系手机号（脱敏：138****8888） */
    private String contactPhone;

    /** 联系QQ号 */
    private String contactQq;

    /** 联系微信号 */
    private String contactWechat;

    // ================== 当前用户相关 ==================

    /** 当前用户是否已收藏此商品（未登录时为 false） */
    private Boolean favorited;

    /** 当前用户是否是卖家 */
    private Boolean isOwner;
}
