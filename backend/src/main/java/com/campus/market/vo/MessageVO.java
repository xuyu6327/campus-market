package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊消息 VO
 */
@Data
@Schema(description = "私聊消息")
public class MessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "发送方用户ID")
    private Long senderId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读：0未读 1已读")
    private Integer isRead;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
