package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊会话实体
 * 对应 im_conversation 表
 */
@Data
@TableName("im_conversation")
public class ImConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品ID（可空，从商品页发起时有） */
    private Long goodsId;

    /** 会话发起方用户ID */
    private Long userAId;

    /** 会话接收方用户ID */
    private Long userBId;

    /** 最后一条消息内容（列表预览） */
    private String lastMessage;

    /** 最后消息时间 */
    private LocalDateTime lastTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ===== 非数据库字段（VO 组装用） =====

    /** 关联商品标题 */
    @TableField(exist = false)
    private String goodsTitle;

    /** 关联商品封面图 */
    @TableField(exist = false)
    private String goodsCoverImage;
}
