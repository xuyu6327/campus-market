package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项 VO
 * 用于商品列表页和搜索结果页展示
 * 只包含列表页需要的精简字段，不包含详细描述和联系方式
 */
@Data
public class GoodsListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** 商品标题 */
    private String title;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色：1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5严重使用痕迹 */
    private Integer goodsCondition;

    /** 成色描述 */
    private String conditionDesc;

    /** 第一张图片URL（封面图） */
    private String coverImage;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 卖家用户ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 卖家头像 */
    private String sellerAvatar;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer favoriteCount;

    /** 状态：0下架 1在售 2预订中 3已售出 4待审核 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 下架方式：0自行下架/正常 1管理员强制下架 */
    private Integer takedownBy;

    /** 强制下架原因（审核驳回原因） */
    private String takedownReason;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 交易地点 */
    private String tradeLocation;
}
