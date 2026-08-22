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
 * 举报实体类
 * 对应数据库表 report
 *
 * 举报对象类型：1=用户 2=商品
 * 处理状态：0待处理 1警告 2下架商品 3封禁账号 4驳回
 *
 * 设计说明：
 * - 该表不使用逻辑删除和乐观锁
 * - 举报提交后不可修改/撤回
 * - 处理状态变更由管理员在后台操作（Step 7）
 */
@Data
@TableName("report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 举报对象类型：1用户 2商品 */
    private Integer targetType;

    /** 举报对象ID */
    private Long targetId;

    /** 举报理由（分类） */
    private String reason;

    /** 详细描述 */
    private String description;

    /** 处理状态：0待处理 1警告 2下架商品 3封禁账号 4驳回 */
    private Integer status;

    /** 处理人ID（管理员） */
    private Long handlerId;

    /** 处理结果说明 */
    private String handleResult;

    /** 举报时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 处理时间 */
    private LocalDateTime handleTime;
}
