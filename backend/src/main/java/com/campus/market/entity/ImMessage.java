package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊消息实体
 * 对应 im_message 表
 */
@Data
@TableName("im_message")
public class ImMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 发送方用户ID */
    private Long senderId;

    /** 消息内容（敏感词过滤后存储） */
    private String content;

    /** 是否已读：0未读 1已读 */
    private Integer isRead;

    /** 发送时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
