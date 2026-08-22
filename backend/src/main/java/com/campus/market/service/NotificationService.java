package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.vo.NotificationVO;

/**
 * 通知服务接口
 *
 * 通知类型：
 * 1=预订提醒 2=取消提醒 3=交易完成 4=评价提醒
 * 5=联系不上提醒 6=卖家已联系 7=系统通知
 */
public interface NotificationService {

    /**
     * 发送通知（内部调用，供其他模块使用）
     *
     * @param userId    接收用户ID
     * @param type      通知类型（1-7）
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID（可为null）
     */
    void sendNotification(Long userId, Integer type, String title, String content, Long relatedId);

    /**
     * 我的通知列表（分页，可按已读状态筛选）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param isRead   已读状态筛选（null=全部, 0=未读, 1=已读）
     */
    Page<NotificationVO> getMyNotifications(Integer pageNum, Integer pageSize, Integer isRead);

    /**
     * 标记单条通知为已读
     */
    void markAsRead(Long notificationId);

    /**
     * 标记全部未读通知为已读
     */
    void markAllAsRead();

    /**
     * 获取未读通知数量
     */
    Integer getUnreadCount();
}
