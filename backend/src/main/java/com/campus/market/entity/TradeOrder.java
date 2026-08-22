package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易订单实体
 * 对应 trade_order 表
 *
 * 状态流转：
 *   0(待交易) ──买家取消──> 2(买家取消)  [买家扣-3信用分]
 *   0(待交易) ──卖家取消──> 3(卖家取消)  [不扣分]
 *   0(待交易) ──卖家确认──> 1(已完成)
 *   0(待交易) ──联系不上24h──> 4(超时自动取消)
 *
 * 关联商品状态：
 *   下单时: goods.status 1→2(预订中)
 *   完成时: goods.status 2→3(已售出)
 *   取消时: goods.status 2→1(在售)
 */
@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号（UUID 去横线，32位唯一） */
    private String orderNo;

    /** 商品ID */
    private Long goodsId;

    /** 买家用户ID */
    private Long buyerId;

    /** 卖家用户ID */
    private Long sellerId;

    /** 买家手机号快照（AES-GCM加密，买家预订时选填冻结） */
    private String buyerPhone;

    /** 买家QQ号快照（买家预订时选填） */
    private String buyerQq;

    /** 买家微信号快照（买家预订时选填） */
    private String buyerWechat;

    /** 卖家QQ号快照（预订时冻结） */
    private String sellerQq;

    /** 卖家微信号快照 */
    private String sellerWechat;

    /**
     * 订单状态：
     * 0=待交易  1=已完成
     * 2=买家取消  3=卖家取消  4=超时自动取消
     */
    private Integer status;

    /** 买家提交"联系不上卖家"的时间（24h后卖家未响应则自动取消） */
    private LocalDateTime contactFailAt;

    /** 实际交易完成时间 */
    private LocalDateTime tradeTime;

    /** 预订时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    // ===== 非数据库字段（用于 VO 组装） =====

    /** 商品标题（关联查询用） */
    @TableField(exist = false)
    private String goodsTitle;

    /** 商品图片（关联查询用） */
    @TableField(exist = false)
    private String goodsImages;

    /** 商品价格（关联查询用） */
    @TableField(exist = false)
    private java.math.BigDecimal goodsPrice;

    /** 买家昵称（关联查询用） */
    @TableField(exist = false)
    private String buyerNickname;

    /** 卖家昵称（关联查询用） */
    @TableField(exist = false)
    private String sellerNickname;
}
