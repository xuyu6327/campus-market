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
 * 信用分变更日志实体类
 * 对应数据库表 credit_log
 *
 * 重要规则（技术文档 v3.0 §3.3）：
 * - 此表只 INSERT，不 UPDATE，不 DELETE（审计要求）
 * - 每次信用分变化都记录变更前/变更后/变更值/原因
 * - operator_id = 0 表示系统自动变更
 */
@Data
@TableName("credit_log")
public class CreditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 变更前信用分 */
    private Integer beforeScore;

    /** 变更后信用分 */
    private Integer afterScore;

    /** 变更分值（正数加分，负数扣分） */
    private Integer changeValue;

    /** 变更原因 */
    private String reason;

    /** 关联订单ID */
    private Long relatedOrderId;

    /** 操作人ID（0=系统自动） */
    private Long operatorId;

    /** 变更时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
