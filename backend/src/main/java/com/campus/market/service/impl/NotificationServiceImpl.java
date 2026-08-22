package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.entity.Notification;
import com.campus.market.mapper.NotificationMapper;
import com.campus.market.service.NotificationService;
import com.campus.market.vo.NotificationVO;
import com.campus.market.util.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 *
 * 核心逻辑：
 * 1. sendNotification: 供其他模块调用，创建通知记录
 * 2. 通知列表: 按已读状态筛选，分页返回
 * 3. 标记已读: 单条/全部
 * 4. 未读数量统计
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    /** 通知类型描述 */
    private static final String[] TYPE_DESCS = {
            null, "预订提醒", "取消提醒", "交易完成", "评价提醒",
            "联系不上提醒", "卖家已联系", "系统通知"
    };

    // ================== 发送通知（内部调用） ==================

    @Override
    public void sendNotification(Long userId, Integer type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0); // 未读
        notification.setRelatedId(relatedId);

        notificationMapper.insert(notification);
        log.info("[通知发送] userId={}, type={}, title={}", userId, type, title);
    }

    // ================== 我的通知列表 ==================

    @Override
    public Page<NotificationVO> getMyNotifications(Integer pageNum, Integer pageSize, Integer isRead) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<Notification> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(isRead != null, Notification::getIsRead, isRead)
                .orderByDesc(Notification::getCreateTime);

        Page<Notification> notifPage = notificationMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<NotificationVO> result = new Page<>(notifPage.getCurrent(), notifPage.getSize(), notifPage.getTotal());
        List<NotificationVO> voList = notifPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    // ================== 标记单条已读 ==================

    @Override
    public void markAsRead(Long notificationId) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BizException(404, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作此通知");
        }
        if (notification.getIsRead() == 1) {
            return; // 已读则幂等返回
        }

        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .set(Notification::getIsRead, 1)
        );
        log.info("[通知已读] userId={}, notificationId={}", userId, notificationId);
    }

    // ================== 标记全部已读 ==================

    @Override
    public void markAllAsRead() {
        Long userId = JwtInterceptor.getCurrentUserId();

        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1)
        );
        log.info("[全部已读] userId={}", userId);
    }

    // ================== 未读数量 ==================

    @Override
    public Integer getUnreadCount() {
        Long userId = JwtInterceptor.getCurrentUserId();

        return Math.toIntExact(notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        ));
    }

    // ================== 工具方法 ==================

    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTypeDesc(getTypeDesc(notification.getType()));
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setRelatedId(notification.getRelatedId());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }

    private String getTypeDesc(Integer type) {
        if (type == null || type < 1 || type > 7) {
            return "未知";
        }
        return TYPE_DESCS[type];
    }
}
