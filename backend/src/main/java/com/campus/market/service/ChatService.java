package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.CreateConversationDTO;
import com.campus.market.dto.SendMessageDTO;
import com.campus.market.vo.ConversationVO;
import com.campus.market.vo.MessageVO;

import java.util.List;

/**
 * 私聊服务接口
 *
 * 功能列表：
 * 1. 发起会话（已存在则复用，返回会话ID）
 * 2. 我的会话列表（分页）
 * 3. 拉取消息（首载最近N条 / afterId 增量轮询，读即已读）
 * 4. 发送消息（敏感词过滤）
 * 5. 私聊未读总数
 */
public interface ChatService {

    /**
     * 发起或复用会话
     * @return 会话ID
     */
    Long createConversation(CreateConversationDTO dto);

    /**
     * 我的会话列表
     */
    Page<ConversationVO> getMyConversations(Integer pageNum, Integer pageSize);

    /**
     * 会话详情（对方昵称/头像、关联商品，聊天窗口顶部展示用）
     */
    ConversationVO getConversationInfo(Long conversationId);

    /**
     * 拉取会话消息
     * @param afterId null=首载（返回最近 limit 条）；否则返回 id > afterId 的新消息（轮询增量）
     * @param limit 首载条数上限
     */
    List<MessageVO> getMessages(Long conversationId, Long afterId, Integer limit);

    /**
     * 发送消息
     */
    void sendMessage(Long conversationId, SendMessageDTO dto);

    /**
     * 我的私聊未读总数（对方发来未读）
     */
    Integer getUnreadCount();
}
