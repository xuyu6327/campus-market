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
 * 通知实体类
 * 对应数据库表 notification
 *
 * 通知类型：
 * 1=预订提醒 2=取消提醒 3=交易完成 4=评价提醒
 * 5=联系不上提醒 6=卖家已联系 7=系统通知
 *
 * 设计说明：
 * - 该表不使用逻辑删除和乐观锁
 * - 通知为追加型数据，只 INSERT 不 UPDATE（除标记已读外）
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知类型：1预订提醒 2取消提醒 3交易完成 4评价提醒 5联系不上提醒 6卖家已联系 7系统通知 */
    private Integer type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 关联业务ID（如订单ID、商品ID） */
    private Long relatedId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
