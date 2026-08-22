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
 * 用户浏览历史实体类
 * 对应数据库表 user_browse_history
 *
 * 设计说明：
 * - (user_id, goods_id) 唯一索引，同一商品只保留一条记录
 * - 用户重复浏览时更新 create_time 为最新浏览时间（通过 INSERT ON DUPLICATE KEY UPDATE 实现）
 * - 该表不使用逻辑删除和乐观锁
 * - 定期清理超过 100 条的旧记录（可在后续优化中实现）
 */
@Data
@TableName("user_browse_history")
public class UserBrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long goodsId;

    /** 浏览时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
