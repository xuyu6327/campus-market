package com.campus.market.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.CreateConversationDTO;
import com.campus.market.dto.SendMessageDTO;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.ImConversation;
import com.campus.market.entity.ImMessage;
import com.campus.market.entity.SysUser;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.ImConversationMapper;
import com.campus.market.mapper.ImMessageMapper;
import com.campus.market.mapper.SysUserMapper;
import com.campus.market.service.ChatService;
import com.campus.market.service.SensitiveWordService;
import com.campus.market.util.PageUtil;
import com.campus.market.vo.ConversationVO;
import com.campus.market.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 私聊服务实现类
 *
 * 核心逻辑：
 * 1. 发起会话：同一商品下双方已有会话则复用（无方向性），否则新建
 * 2. 拉取消息：首载取最近 N 条，轮询传 afterId 取增量；拉取后自动把对方消息标记已读
 * 3. 发送消息：敏感词过滤，更新会话最后消息
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ImConversationMapper imConversationMapper;

    @Autowired
    private ImMessageMapper imMessageMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    // ================== 发起/复用会话 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(CreateConversationDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long targetUserId = dto.getTargetUserId();

        if (targetUserId.equals(userId)) {
            throw new BizException(400, "不能和自己发起会话");
        }
        SysUser target = sysUserMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException(404, "对方用户不存在");
        }
        // 关联商品存在性校验（可选字段）
        GoodsInfo goods = null;
        if (dto.getGoodsId() != null) {
            goods = goodsInfoMapper.selectById(dto.getGoodsId());
            if (goods == null) {
                throw new BizException(404, "商品不存在");
            }
        }

        // 查已有会话（无方向性：A-B 与 B-A 视为同一会话）
        Long goodsId = dto.getGoodsId();
        ImConversation existing = imConversationMapper.selectOne(
                new LambdaQueryWrapper<ImConversation>()
                        .eq(goodsId != null, ImConversation::getGoodsId, goodsId)
                        .and(w -> w
                                .and(x -> x.eq(ImConversation::getUserAId, userId).eq(ImConversation::getUserBId, targetUserId))
                                .or(y -> y.eq(ImConversation::getUserAId, targetUserId).eq(ImConversation::getUserBId, userId)))
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing.getId();
        }

        ImConversation conv = new ImConversation();
        conv.setGoodsId(goodsId);
        conv.setUserAId(userId);
        conv.setUserBId(targetUserId);
        imConversationMapper.insert(conv);
        log.info("[发起会话] userId={}, targetUserId={}, goodsId={}, convId={}", userId, targetUserId, goodsId, conv.getId());
        return conv.getId();
    }

    // ================== 我的会话列表 ==================

    @Override
    public Page<ConversationVO> getMyConversations(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<ImConversation> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<ImConversation>()
                .and(w -> w.eq(ImConversation::getUserAId, userId).or().eq(ImConversation::getUserBId, userId))
                .orderByDesc(ImConversation::getLastTime)
                .orderByDesc(ImConversation::getId);
        Page<ImConversation> convPage = imConversationMapper.selectPage(page, wrapper);

        Page<ConversationVO> result = new Page<>(convPage.getCurrent(), convPage.getSize(), convPage.getTotal());
        List<ConversationVO> voList = convPage.getRecords().stream()
                .map(conv -> convertToConversationVO(conv, userId))
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    // ================== 会话详情 ==================

    @Override
    public ConversationVO getConversationInfo(Long conversationId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        ImConversation conv = requireMember(conversationId, userId);
        return convertToConversationVO(conv, userId);
    }

    // ================== 拉取消息（首载/增量，读即已读） ==================

    @Override
    public List<MessageVO> getMessages(Long conversationId, Long afterId, Integer limit) {
        Long userId = JwtInterceptor.getCurrentUserId();
        ImConversation conv = requireMember(conversationId, userId);

        List<ImMessage> messages;
        if (afterId == null || afterId <= 0) {
            // 首载：最近 limit 条（默认 50），按 id 升序返回
            int size = (limit == null || limit <= 0) ? 50 : Math.min(limit, 100);
            List<ImMessage> recent = imMessageMapper.selectList(
                    new LambdaQueryWrapper<ImMessage>()
                            .eq(ImMessage::getConversationId, conversationId)
                            .orderByDesc(ImMessage::getId)
                            .last("LIMIT " + size)
            );
            Collections.reverse(recent);
            messages = recent;
        } else {
            // 增量轮询：id > afterId
            messages = imMessageMapper.selectList(
                    new LambdaQueryWrapper<ImMessage>()
                            .eq(ImMessage::getConversationId, conversationId)
                            .gt(ImMessage::getId, afterId)
                            .orderByAsc(ImMessage::getId)
            );
        }

        // 读即已读：把对方发来的未读消息标记已读
        Long unreadMarked = imMessageMapper.selectCount(
                new LambdaQueryWrapper<ImMessage>()
                        .eq(ImMessage::getConversationId, conversationId)
                        .ne(ImMessage::getSenderId, userId)
                        .eq(ImMessage::getIsRead, 0)
        );
        if (unreadMarked != null && unreadMarked > 0) {
            imMessageMapper.update(null,
                    new LambdaUpdateWrapper<ImMessage>()
                            .eq(ImMessage::getConversationId, conversationId)
                            .ne(ImMessage::getSenderId, userId)
                            .eq(ImMessage::getIsRead, 0)
                            .set(ImMessage::getIsRead, 1)
            );
        }

        return messages.stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setSenderId(m.getSenderId());
            vo.setContent(m.getContent());
            vo.setIsRead(m.getIsRead());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    // ================== 发送消息 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(Long conversationId, SendMessageDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        ImConversation conv = requireMember(conversationId, userId);

        String content = dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BizException(400, "消息内容不能为空");
        }
        if (sensitiveWordService.containsSensitive(content)) {
            throw new BizException(400, "消息包含敏感词，请修改后重新发送");
        }

        ImMessage msg = new ImMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(userId);
        msg.setContent(content);
        msg.setIsRead(0);
        imMessageMapper.insert(msg);

        // 更新会话最后消息
        conv.setLastMessage(content);
        conv.setLastTime(LocalDateTime.now());
        imConversationMapper.updateById(conv);
    }

    // ================== 未读总数 ==================

    @Override
    public Integer getUnreadCount() {
        Long userId = JwtInterceptor.getCurrentUserId();

        List<ImConversation> convs = imConversationMapper.selectList(
                new LambdaQueryWrapper<ImConversation>()
                        .and(w -> w.eq(ImConversation::getUserAId, userId).or().eq(ImConversation::getUserBId, userId))
        );
        int total = 0;
        for (ImConversation conv : convs) {
            Long unread = imMessageMapper.selectCount(
                    new LambdaQueryWrapper<ImMessage>()
                            .eq(ImMessage::getConversationId, conv.getId())
                            .ne(ImMessage::getSenderId, userId)
                            .eq(ImMessage::getIsRead, 0)
            );
            total += (unread != null ? unread.intValue() : 0);
        }
        return total;
    }

    // ================== 工具方法 ==================

    /**
     * 校验会话存在且为成员，返回会话
     */
    private ImConversation requireMember(Long conversationId, Long userId) {
        ImConversation conv = imConversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BizException(404, "会话不存在");
        }
        boolean member = conv.getUserAId().equals(userId) || conv.getUserBId().equals(userId);
        if (!member) {
            throw new BizException(403, "无权访问该会话");
        }
        return conv;
    }

    /**
     * 会话 -> VO（含对方信息、商品信息、未读数）
     */
    private ConversationVO convertToConversationVO(ImConversation conv, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        vo.setGoodsId(conv.getGoodsId());
        vo.setLastMessage(conv.getLastMessage());
        vo.setLastTime(conv.getLastTime());

        Long otherId = conv.getUserAId().equals(currentUserId) ? conv.getUserBId() : conv.getUserAId();
        vo.setOtherId(otherId);
        SysUser other = sysUserMapper.selectById(otherId);
        if (other != null) {
            vo.setOtherNickname(other.getNickname());
            vo.setOtherAvatar(other.getAvatar());
        }

        if (conv.getGoodsId() != null) {
            GoodsInfo goods = goodsInfoMapper.selectById(conv.getGoodsId());
            if (goods != null) {
                vo.setGoodsTitle(goods.getTitle());
                if (goods.getImages() != null && !goods.getImages().isEmpty()) {
                    List<String> images = JSONUtil.toList(goods.getImages(), String.class);
                    if (images != null && !images.isEmpty()) {
                        vo.setGoodsCoverImage(images.get(0));
                    }
                }
            }
        }

        // 对方发来的未读数
        Long unread = imMessageMapper.selectCount(
                new LambdaQueryWrapper<ImMessage>()
                        .eq(ImMessage::getConversationId, conv.getId())
                        .ne(ImMessage::getSenderId, currentUserId)
                        .eq(ImMessage::getIsRead, 0)
        );
        vo.setUnreadCount(unread != null ? unread.intValue() : 0);
        return vo;
    }
}
