package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台商品管理VO
 * 管理员可见完整商品信息（含卖家信息）
 */
@Data
public class AdminGoodsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** 卖家用户ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色 */
    private Integer goodsCondition;

    /** 成色描述 */
    private String conditionDesc;

    /** 图片URL列表（JSON数组） */
    private String images;

    /** 交易地点 */
    private String tradeLocation;

    /** 联系方式类型 */
    private Integer contactMethod;

    /** 联系QQ号 */
    private String contactQq;

    /** 联系微信号 */
    private String contactWechat;

    /** 联系手机号（管理员可见解密后） */
    private String contactPhone;

    /** 状态：0下架 1在售 2预订中 3已售出 4待审核 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 下架方式：0自行下架/正常 1管理员强制下架 */
    private Integer takedownBy;

    /** 强制下架原因（审核驳回原因） */
    private String takedownReason;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer favoriteCount;

    /** 最近上架时间 */
    private LocalDateTime lastRelistedAt;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
