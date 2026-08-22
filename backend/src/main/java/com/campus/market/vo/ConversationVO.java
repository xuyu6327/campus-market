package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊会话 VO（会话列表用）
 */
@Data
@Schema(description = "私聊会话信息")
public class ConversationVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "对方用户ID")
    private Long otherId;

    @Schema(description = "对方昵称")
    private String otherNickname;

    @Schema(description = "对方头像")
    private String otherAvatar;

    @Schema(description = "关联商品ID")
    private Long goodsId;

    @Schema(description = "关联商品标题")
    private String goodsTitle;

    @Schema(description = "关联商品封面图")
    private String goodsCoverImage;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastTime;

    @Schema(description = "对方发来的未读消息数")
    private Integer unreadCount;
}
