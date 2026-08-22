package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 发起私聊会话 DTO
 * 对应接口：POST /chat/conversation
 */
@Data
public class CreateConversationDTO {

    /** 关联商品ID（可空，从商品页发起时有） */
    private Long goodsId;

    /** 对方用户ID（必填） */
    @NotNull(message = "对方用户ID不能为空")
    private Long targetUserId;
}
