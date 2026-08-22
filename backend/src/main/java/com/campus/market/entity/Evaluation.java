package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价实体类
 * 对应数据库表 evaluation
 *
 * 设计说明：
 * - (order_id, evaluator_id) 唯一索引，一个订单中一个角色只能评价一次
 * - evaluator_role: 1=买家评卖家, 2=卖家评买家
 * - 匿名评价（is_anonymous=1）时前端展示脱敏昵称
 * - 该表不使用逻辑删除和乐观锁
 */
@Data
@TableName("evaluation")
public class Evaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long goodsId;

    /** 评价人ID */
    private Long evaluatorId;

    /** 被评价人ID */
    private Long evaluateeId;

    /** 评价人角色：1买家评卖家 2卖家评买家 */
    private Integer evaluatorRole;

    /** 评分：1-5星 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 是否匿名：0实名 1匿名 */
    private Integer isAnonymous;

    /** 状态：0隐藏 1正常 2申诉中 3申诉后隐藏 */
    private Integer status;

    /** 评价时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
