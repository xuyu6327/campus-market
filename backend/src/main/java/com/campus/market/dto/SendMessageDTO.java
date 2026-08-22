package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送私聊消息 DTO
 * 对应接口：POST /chat/{conversationId}/messages
 */
@Data
public class SendMessageDTO {

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容最多500字符")
    private String content;
}
