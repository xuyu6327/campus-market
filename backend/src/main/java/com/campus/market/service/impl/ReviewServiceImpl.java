package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.CreateReviewDTO;
import com.campus.market.entity.Evaluation;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.SysUser;
import com.campus.market.entity.TradeOrder;
import com.campus.market.mapper.EvaluationMapper;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.SysUserMapper;
import com.campus.market.mapper.TradeOrderMapper;
import com.campus.market.service.NotificationService;
import com.campus.market.service.ReviewService;
import com.campus.market.service.SensitiveWordService;
import com.campus.market.service.SysUserService;
import com.campus.market.vo.ReviewVO;
import com.campus.market.vo.UserReviewVO;
import com.campus.market.util.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 评价服务实现类
 *
 * 核心逻辑：
 * 1. 创建评价：校验订单已完成 + 身份校验 + 防重复 -> 插入评价 + 发送通知
 * 2. 商品评价列表：公开查看，匿名评价脱敏展示
 * 3. 我发出/收到的评价：个人维度查看
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private SysUserService sysUserService;

    @Value("${campus.market.credit.good-review}")
    private Integer goodReviewBonus;

    @Value("${campus.market.credit.bad-review}")
    private Integer badReviewPenalty;

    /** 评价状态描述 */
    private static final String[] REVIEW_STATUS_DESCS = {
            "隐藏", "正常", "申诉中", "申诉后隐藏"
    };

    // ================== 创建评价 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(CreateReviewDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[创建评价] userId={}, orderId={}", userId, dto.getOrderId());

        // 1. 查询订单
        TradeOrder order = tradeOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BizException(400, "仅已完成的订单可以评价");
        }

        // 2. 判断评价人身份
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BizException(403, "只有该订单的买家或卖家可以评价");
        }

        // 2.5 敏感词过滤（评价内容）
        if (dto.getContent() != null && !dto.getContent().isEmpty()
                && sensitiveWordService.containsSensitive(dto.getContent())) {
            throw new BizException(400, "评价内容包含敏感词，请修改后重新提交");
        }

        // 3. 防重复评价（唯一索引 order_id + evaluator_id）
        Long existCount = evaluationMapper.selectCount(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, dto.getOrderId())
                        .eq(Evaluation::getEvaluatorId, userId)
        );
        if (existCount > 0) {
            throw new BizException(400, "您已对该订单评价过，不能重复评价");
        }

        // 4. 构建评价记录
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(order.getId());
        evaluation.setGoodsId(order.getGoodsId());
        evaluation.setEvaluatorId(userId);
        evaluation.setEvaluateeId(isBuyer ? order.getSellerId() : order.getBuyerId());
        evaluation.setEvaluatorRole(isBuyer ? 1 : 2);
        evaluation.setScore(dto.getScore());
        evaluation.setContent(dto.getContent());
        evaluation.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : 1);
        evaluation.setStatus(1); // 正常

        evaluationMapper.insert(evaluation);
        log.info("[评价创建成功] evaluationId={}, orderId={}, evaluatorRole={}",
                evaluation.getId(), dto.getOrderId(), evaluation.getEvaluatorRole());

        // 5. 评价信用分联动：好评加分，差评扣分（中评不加减）
        int creditChange = 0;
        String creditReason = "";
        if (dto.getScore() != null && dto.getScore() >= 4) {
            creditChange = goodReviewBonus;
            creditReason = "获得好评";
        } else if (dto.getScore() != null && dto.getScore() <= 2) {
            creditChange = badReviewPenalty;
            creditReason = "获得差评";
        }
        if (creditChange != 0) {
            try {
                sysUserService.updateCreditScore(
                        evaluation.getEvaluateeId(), creditChange, creditReason, order.getId(), 0L
                );
            } catch (Exception e) {
                log.error("[评价信用分联动失败] evaluateeId={}, orderId={}", evaluation.getEvaluateeId(), order.getId(), e);
            }
        }

        // 6. 发送通知给被评价人
        String roleDesc = isBuyer ? "买家" : "卖家";
        notificationService.sendNotification(
                evaluation.getEvaluateeId(),
                4, // 评价提醒
                "收到新评价",
                "您的交易对象给您打出了" + dto.getScore() + "星评价",
                order.getId()
        );

        return evaluation.getId();
    }

    // ================== 商品评价列表（公开，含双盲判断） ==================

    @Override
    public Page<ReviewVO> getGoodsReviews(Long goodsId, Integer pageNum, Integer pageSize) {
        Page<Evaluation> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getGoodsId, goodsId)
                .eq(Evaluation::getStatus, 1) // 仅正常状态
                .eq(Evaluation::getEvaluatorRole, 1) // 商品详情页只展示买家对卖家的评价
                .orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> evalPage = evaluationMapper.selectPage(page, wrapper);
        if (evalPage.getRecords().isEmpty()) {
            return convertToReviewVOPage(evalPage, null, new HashMap<>());
        }

        // 双盲判断：收集订单ID -> 查每个订单的评价数量 + 交易完成时间
        List<Long> orderIds = evalPage.getRecords().stream()
                .map(Evaluation::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        // 每个订单的评价数量（判断双方是否都评了）
        List<Evaluation> allEvals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .in(Evaluation::getOrderId, orderIds)
        );
        Map<Long, Long> evalCountByOrder = allEvals.stream()
                .collect(Collectors.groupingBy(Evaluation::getOrderId, Collectors.counting()));

        // 每个订单的交易完成时间
        Map<Long, LocalDateTime> tradeTimeByOrder = new HashMap<>();
        for (Long orderId : orderIds) {
            TradeOrder order = tradeOrderMapper.selectById(orderId);
            if (order != null && order.getTradeTime() != null) {
                tradeTimeByOrder.put(orderId, order.getTradeTime());
            }
        }

        // 判断每条评价是否公开可见
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Boolean> revealByOrder = new HashMap<>();
        for (Long orderId : orderIds) {
            boolean bothRated = evalCountByOrder.getOrDefault(orderId, 0L) >= 2;
            LocalDateTime tradeTime = tradeTimeByOrder.get(orderId);
            boolean pastWindow = tradeTime != null && tradeTime.plusDays(7).isBefore(now);
            revealByOrder.put(orderId, bothRated || pastWindow);
        }

        return convertToReviewVOPage(evalPage, null, revealByOrder);
    }

    // ================== 用户收到的评价（含好评率） ==================

    @Override
    public UserReviewVO getUserReviews(Long userId, Integer pageNum, Integer pageSize) {
        Page<Evaluation> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluateeId, userId)
                .eq(Evaluation::getStatus, 1)
                .orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> evalPage = evaluationMapper.selectPage(page, wrapper);

        // 好评率统计
        Long totalCount = evaluationMapper.selectCount(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluateeId, userId)
                        .eq(Evaluation::getStatus, 1)
        );

        UserReviewVO vo = new UserReviewVO();
        vo.setTotalCount(totalCount);
        if (totalCount > 0) {
            Long goodCount = evaluationMapper.selectCount(
                    new LambdaQueryWrapper<Evaluation>()
                            .eq(Evaluation::getEvaluateeId, userId)
                            .eq(Evaluation::getStatus, 1)
                            .ge(Evaluation::getScore, 4)
            );
            vo.setGoodRate((int) Math.round(goodCount * 100.0 / totalCount));
        } else {
            vo.setGoodRate(100);
        }

        // 收到的评价全部可见（revealByOrder=null）
        Page<ReviewVO> reviewPage = convertToReviewVOPage(evalPage, null, null);
        vo.setRecords(reviewPage.getRecords());
        return vo;
    }

    // ================== 我发出的评价 ==================

    @Override
    public Page<ReviewVO> getMySentReviews(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<Evaluation> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluatorId, userId)
                .orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> evalPage = evaluationMapper.selectPage(page, wrapper);
        return convertToReviewVOPage(evalPage, userId, null);
    }

    // ================== 我收到的评价 ==================

    @Override
    public Page<ReviewVO> getMyReceivedReviews(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<Evaluation> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluateeId, userId)
                .orderByDesc(Evaluation::getCreateTime);

        Page<Evaluation> evalPage = evaluationMapper.selectPage(page, wrapper);
        return convertToReviewVOPage(evalPage, userId, null);
    }

    // ================== 工具方法 ==================

    /**
     * 将 Evaluation Page 转换为 ReviewVO Page
     * @param currentUserId 当前登录用户ID（null 表示未登录/公开查看）
     * @param revealByOrder 订单ID -> 是否公开可见（null 表示全部可见，如个人中心自己的评价）
     */
    private Page<ReviewVO> convertToReviewVOPage(Page<Evaluation> evalPage, Long currentUserId, Map<Long, Boolean> revealByOrder) {
        Page<ReviewVO> result = new Page<>(evalPage.getCurrent(), evalPage.getSize(), evalPage.getTotal());
        List<ReviewVO> voList = evalPage.getRecords().stream()
                .map(evaluation -> convertToReviewVO(evaluation, currentUserId, revealByOrder))
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    /**
     * 将 Evaluation 转换为 ReviewVO
     */
    private ReviewVO convertToReviewVO(Evaluation evaluation, Long currentUserId, Map<Long, Boolean> revealByOrder) {
        ReviewVO vo = new ReviewVO();
        vo.setId(evaluation.getId());
        vo.setOrderId(evaluation.getOrderId());
        vo.setGoodsId(evaluation.getGoodsId());
        vo.setEvaluatorRole(evaluation.getEvaluatorRole());
        vo.setIsAnonymous(evaluation.getIsAnonymous());
        vo.setStatus(evaluation.getStatus());
        vo.setStatusDesc(getReviewStatusDesc(evaluation.getStatus()));
        vo.setCreateTime(evaluation.getCreateTime());

        // 双盲判断：仅一方评价且未过7天窗口时，隐藏评分和内容
        boolean reveal = revealByOrder == null
                || revealByOrder.getOrDefault(evaluation.getOrderId(), true);
        if (reveal) {
            vo.setScore(evaluation.getScore());
            vo.setContent(evaluation.getContent());
        } else {
            vo.setScore(null);
            vo.setContent("对方评价后可见");
        }

        // 查询商品标题
        GoodsInfo goods = goodsInfoMapper.selectById(evaluation.getGoodsId());
        if (goods != null) {
            vo.setGoodsTitle(goods.getTitle());
        }

        // 查询评价人昵称
        SysUser evaluator = sysUserMapper.selectById(evaluation.getEvaluatorId());
        if (evaluator != null) {
            // 匿名评价时脱敏：显示首字 + ***
            if (evaluation.getIsAnonymous() == 1) {
                vo.setEvaluatorNickname(maskNickname(evaluator.getNickname()));
            } else {
                vo.setEvaluatorNickname(evaluator.getNickname());
            }
        }

        // 查询被评价人昵称
        SysUser evaluatee = sysUserMapper.selectById(evaluation.getEvaluateeId());
        if (evaluatee != null) {
            if (evaluation.getIsAnonymous() == 1) {
                vo.setEvaluateeNickname(maskNickname(evaluatee.getNickname()));
            } else {
                vo.setEvaluateeNickname(evaluatee.getNickname());
            }
        }

        // 设置当前用户身份标记
        if (currentUserId != null) {
            vo.setIsEvaluator(evaluation.getEvaluatorId().equals(currentUserId));
            vo.setIsEvaluatee(evaluation.getEvaluateeId().equals(currentUserId));
        } else {
            vo.setIsEvaluator(false);
            vo.setIsEvaluatee(false);
        }

        return vo;
    }

    /**
     * 昵称脱敏：保留首字，其余用 * 替代
     * 例：小明 -> 小*，张三丰 -> 张**
     */
    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "匿名用户";
        }
        if (nickname.length() == 1) {
            return nickname;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nickname.charAt(0));
        for (int i = 1; i < nickname.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    /**
     * 获取评价状态描述
     */
    private String getReviewStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 3) {
            return "未知";
        }
        return REVIEW_STATUS_DESCS[status];
    }
}
