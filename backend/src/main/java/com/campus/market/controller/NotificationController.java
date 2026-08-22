package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.service.NotificationService;
import com.campus.market.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 *
 * 接口列表：
 * - GET  /notification/list          我的通知列表（分页，可筛选已读状态）
 * - PUT  /notification/{id}/read     标记单条已读
 * - PUT  /notification/read-all      标记全部已读
 * - GET  /notification/unread-count  获取未读数量
 */
@RestController
@RequestMapping("/notification")
@Tag(name = "通知管理", description = "通知查看与已读管理")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    @Operation(summary = "我的通知列表", description = "分页查询当前用户的通知，可通过 isRead 参数筛选未读/已读")
    public Result<Page<NotificationVO>> getMyNotifications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        Page<NotificationVO> page = notificationService.getMyNotifications(pageNum, pageSize, isRead);
        return Result.success(page);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读", description = "将指定通知标记为已读")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记全部已读", description = "将当前用户所有未读通知标记为已读")
    public Result<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return Result.success();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量", description = "返回当前用户的未读通知数量")
    public Result<Integer> getUnreadCount() {
        Integer count = notificationService.getUnreadCount();
        return Result.success(count);
    }
}
