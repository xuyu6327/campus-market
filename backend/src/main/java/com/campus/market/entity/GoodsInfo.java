package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息实体类
 * 对应数据库表 goods_info
 *
 * 关键设计：
 * - seller_id 关联卖家用户
 * - images 字段存储 JSON 数组格式的图片URL列表
 * - contact_phone 字段 AES-GCM 加密存储（与 sys_user.phone 同一加密方案）
 * - status: 0下架 1在售 2预订中 3已售出
 * - last_relisted_at: 最近上架时间，30天自动下架按此字段计算
 * - version: 乐观锁版本号，配合 @Version 注解
 * - deleted: 逻辑删除标记
 */
@Data
@TableName("goods_info")
public class GoodsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家用户ID */
    private Long sellerId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 分类ID */
    private Long categoryId;

    /** 售价 */
    private BigDecimal price;

    /** 原价（展示用） */
    private BigDecimal originalPrice;

    /** 成色：1全新 2几乎全新 3轻微使用痕迹 4明显使用痕迹 5严重使用痕迹 */
    private Integer goodsCondition;

    /** 图片URL列表（JSON数组格式） */
    private String images;

    /** 交易地点 */
    private String tradeLocation;

    /** 联系方式类型：1手机 2QQ 3微信 */
    private Integer contactMethod;

    /** 联系QQ号 */
    private String contactQq;

    /** 联系微信号 */
    private String contactWechat;

    /** 联系手机号（AES-GCM加密存储） */
    private String contactPhone;

    /** 状态：0下架 1在售 2预订中 3已售出 4待审核（强制下架后卖家修改提交，等管理员审核） */
    private Integer status;

    /** 下架方式：0自行下架/正常 1管理员强制下架 */
    private Integer takedownBy;

    /** 强制下架原因（审核驳回原因） */
    private String takedownReason;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer favoriteCount;

    /** 最近上架时间（30天自动下架按此字段计算） */
    private LocalDateTime lastRelistedAt;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 发布时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
