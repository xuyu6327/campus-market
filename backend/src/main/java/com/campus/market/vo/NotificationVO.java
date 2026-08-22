package com.campus.market.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 VO
 */
@Data
@Schema(description = "通知信息")
public class NotificationVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知类型：1预订提醒 2取消提醒 3交易完成 4评价提醒 5联系不上提醒 6卖家已联系 7系统通知")
    private Integer type;

    @Schema(description = "通知类型描述")
    private String typeDesc;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "是否已读：0未读 1已读")
    private Integer isRead;

    @Schema(description = "关联业务ID（如订单ID、商品ID）")
    private Long relatedId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
