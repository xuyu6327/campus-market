package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台订单管理VO
 * 管理员可见完整订单信息（含买家/卖家信息）
 */
@Data
public class AdminOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 商品ID */
    private Long goodsId;

    /** 商品标题 */
    private String goodsTitle;

    /** 商品图片（第一张） */
    private String goodsImage;

    /** 商品价格 */
    private BigDecimal goodsPrice;

    /** 买家用户ID */
    private Long buyerId;

    /** 买家昵称 */
    private String buyerNickname;

    /** 卖家用户ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 买家手机号（解密后） */
    private String buyerPhone;

    /** 卖家QQ号 */
    private String sellerQq;

    /** 卖家微信号 */
    private String sellerWechat;

    /** 订单状态：0待交易 1已完成 2买家取消 3卖家取消 4超时自动取消 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 联系不上卖家提交时间 */
    private LocalDateTime contactFailAt;

    /** 交易完成时间 */
    private LocalDateTime tradeTime;

    /** 预订时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
