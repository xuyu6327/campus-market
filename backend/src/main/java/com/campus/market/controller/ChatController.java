package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.CreateConversationDTO;
import com.campus.market.dto.SendMessageDTO;
import com.campus.market.service.ChatService;
import com.campus.market.vo.ConversationVO;
import com.campus.market.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私聊控制器
 *
 * 接口列表（全部需登录）：
 * - POST /chat/conversation             发起会话（已存在则复用）
 * - GET  /chat/conversations            我的会话列表（分页）
 * - GET  /chat/{convId}/messages        拉取消息（afterId 增量轮询，读即已读）
 * - POST /chat/{convId}/messages        发送消息
 * - GET  /chat/unread-count             私聊未读总数
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@Tag(name = "私聊模块", description = "用户间私聊：会话/消息/未读")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/conversation")
    @Operation(summary = "发起会话", description = "同一商品下双方已有会话则复用，返回会话ID")
    public Result<Long> createConversation(@Validated @RequestBody CreateConversationDTO dto) {
        return Result.success(chatService.createConversation(dto));
    }

    @GetMapping("/conversations")
    @Operation(summary = "我的会话列表", description = "分页获取我参与的私聊会话")
    public Result<Page<ConversationVO>> getMyConversations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(chatService.getMyConversations(pageNum, pageSize));
    }

    @GetMapping("/{convId}/info")
    @Operation(summary = "会话详情", description = "对方昵称/头像、关联商品信息（聊天窗口顶部展示）")
    public Result<ConversationVO> getConversationInfo(@PathVariable Long convId) {
        return Result.success(chatService.getConversationInfo(convId));
    }

    @GetMapping("/{convId}/messages")
    @Operation(summary = "拉取消息", description = "不传 afterId 返回最近50条；传 afterId 返回其后的新消息（轮询），拉取后自动标记对方消息已读")
    public Result<List<MessageVO>> getMessages(
            @PathVariable Long convId,
            @RequestParam(required = false) Long afterId) {
        return Result.success(chatService.getMessages(convId, afterId, 50));
    }

    @PostMapping("/{convId}/messages")
    @Operation(summary = "发送消息", description = "内容经敏感词过滤后存储")
    public Result<Void> sendMessage(@PathVariable Long convId, @Validated @RequestBody SendMessageDTO dto) {
        chatService.sendMessage(convId, dto);
        return Result.success("发送成功", null);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "私聊未读总数", description = "所有会话中对方发来未读的消息数")
    public Result<Integer> getUnreadCount() {
        return Result.success(chatService.getUnreadCount());
    }
}
