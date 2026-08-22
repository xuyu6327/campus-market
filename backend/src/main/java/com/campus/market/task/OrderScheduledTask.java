package com.campus.market.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.entity.Evaluation;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.TradeOrder;
import com.campus.market.mapper.EvaluationMapper;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.TradeOrderMapper;
import com.campus.market.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 *
 * 功能：
 * - "联系不上卖家"超时自动取消：
 *   买家提交"联系不上卖家"后，超过 contact-fail-hours（默认24小时）卖家仍未响应，
 *   系统自动取消订单，商品恢复"在售"状态。
 *
 * 状态流转：
 *   0(待交易) + contactFailAt != null + 超过24h --> 4(超时自动取消)
 *   对应商品状态 2(预订中) --> 1(在售)
 */
@Slf4j
@Component
public class OrderScheduledTask {

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private com.campus.market.service.NotificationService notificationService;

    @Value("${campus.market.order.contact-fail-hours}")
    private Integer contactFailHours;

    @Value("${campus.market.order.timeout-minutes}")
    private Integer timeoutMinutes;

    @Value("${campus.market.credit.buyer-cancel}")
    private Integer buyerCancelPenalty;

    @Value("${campus.market.credit.good-review}")
    private Integer goodReviewBonus;

    /**
     * "联系不上卖家"超时自动取消
     * cron: 每小时执行一次（0分0秒）
     * 0 秒 0 分 * 时 * * ?  （秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCancelContactFailOrders() {
        log.info("[定时任务] 开始检查\"联系不上卖家\"超时订单，阈值={}小时", contactFailHours);

        // 1. 计算截止时间：当前时间 - contactFailHours
        LocalDateTime deadline = LocalDateTime.now().minusHours(contactFailHours);

        // 2. 查询需要自动取消的订单
        //    状态=0(待交易) + contactFailAt != null + contactFailAt < deadline
        List<TradeOrder> timeoutOrders = tradeOrderMapper.selectList(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, 0)
                        .isNotNull(TradeOrder::getContactFailAt)
                        .lt(TradeOrder::getContactFailAt, deadline)
        );

        if (timeoutOrders.isEmpty()) {
            log.info("[定时任务] 没有需要自动取消的订单");
            return;
        }

        log.info("[定时任务] 发现{}个超时订单需要自动取消", timeoutOrders.size());

        // 3. 逐个处理
        int count = 0;
        for (TradeOrder order : timeoutOrders) {
            try {
                // 3.1 更新订单状态为"超时自动取消"
                order.setStatus(4);
                int rows = tradeOrderMapper.updateById(order);
                if (rows == 0) {
                    log.warn("[定时任务] 订单状态更新失败（乐观锁冲突），跳过: orderId={}", order.getId());
                    continue;
                }

                // 3.2 商品状态 2(预订中) -> 1(在售)
                goodsInfoMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GoodsInfo>()
                                .eq(GoodsInfo::getId, order.getGoodsId())
                                .eq(GoodsInfo::getStatus, 2)
                                .set(GoodsInfo::getStatus, 1)
                );

                // 3.3 联系不上卖家超时不扣分（责任在卖家失联，不扣买家）

                // 3.4 通知买卖双方订单已超时取消
                try {
                    GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
                    String goodsTitle = goods != null ? goods.getTitle() : "商品";
                    // 通知买家
                    notificationService.sendNotification(
                            order.getBuyerId(), 2,
                            "订单已超时取消",
                            "您预订的「" + goodsTitle + "」因长时间未完成交易已自动取消，商品已恢复在售",
                            order.getId()
                    );
                    // 通知卖家
                    notificationService.sendNotification(
                            order.getSellerId(), 2,
                            "订单已超时取消",
                            "买家预订的「" + goodsTitle + "」因长时间未完成交易已自动取消，商品已恢复在售",
                            order.getId()
                    );
                } catch (Exception e) {
                    log.error("[定时任务] 超时取消通知发送失败: orderId={}", order.getId(), e);
                }

                log.info("[定时任务] 订单超时自动取消: orderId={}, orderNo={}, goodsId={}, contactFailAt={}",
                        order.getId(), order.getOrderNo(), order.getGoodsId(), order.getContactFailAt());
                count++;
            } catch (Exception e) {
                log.error("[定时任务] 订单自动取消异常: orderId={}", order.getId(), e);
            }
        }

        log.info("[定时任务] \"联系不上卖家\"超时自动取消完成，共处理{}个订单", count);
    }

    /**
     * 预订超时自动释放
     * 买家预订后 timeout-minutes 分钟内未确认/取消交易，系统自动释放，商品恢复在售
     * cron: 每15分钟执行一次
     */
    @Scheduled(cron = "0 */15 * * * ?")
    public void autoReleaseTimeoutBookings() {
        log.info("[定时任务] 开始检查预订超时订单，阈值={}分钟", timeoutMinutes);

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 查询预订超时订单：状态=0(待交易) + 未提交"联系不上" + 预订时间早于截止时间
        List<TradeOrder> timeoutOrders = tradeOrderMapper.selectList(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, 0)
                        .isNull(TradeOrder::getContactFailAt)
                        .lt(TradeOrder::getCreateTime, deadline)
        );

        if (timeoutOrders.isEmpty()) {
            log.info("[定时任务] 没有需要释放的预订超时订单");
            return;
        }

        int count = 0;
        for (TradeOrder order : timeoutOrders) {
            try {
                // 1. 更新订单状态为"超时自动取消"
                order.setStatus(4);
                int rows = tradeOrderMapper.updateById(order);
                if (rows == 0) {
                    log.warn("[定时任务] 订单状态更新失败（乐观锁冲突），跳过: orderId={}", order.getId());
                    continue;
                }

                // 2. 商品状态 2(预订中) -> 1(在售)
                goodsInfoMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GoodsInfo>()
                                .eq(GoodsInfo::getId, order.getGoodsId())
                                .eq(GoodsInfo::getStatus, 2)
                                .set(GoodsInfo::getStatus, 1)
                );

                // 3. 买家信用分 -3
                try {
                    sysUserService.updateCreditScore(
                            order.getBuyerId(), buyerCancelPenalty,
                            "预订超时自动释放", order.getId(), 0L
                    );
                } catch (Exception e) {
                    log.error("[定时任务] 预订超时信用分扣减失败: buyerId={}, orderId={}", order.getBuyerId(), order.getId(), e);
                }

                // 4. 通知买卖双方
                try {
                    GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
                    String goodsTitle = goods != null ? goods.getTitle() : "商品";
                    notificationService.sendNotification(
                            order.getBuyerId(), 2,
                            "预订已超时释放",
                            "您预订的「" + goodsTitle + "」因超时未完成交易已自动释放，商品已恢复在售",
                            order.getId()
                    );
                    notificationService.sendNotification(
                            order.getSellerId(), 2,
                            "预订已超时释放",
                            "买家预订的「" + goodsTitle + "」因超时未完成交易已自动释放，商品已恢复在售",
                            order.getId()
                    );
                } catch (Exception e) {
                    log.error("[定时任务] 预订超时通知发送失败: orderId={}", order.getId(), e);
                }

                log.info("[定时任务] 预订超时自动释放: orderId={}, orderNo={}, goodsId={}",
                        order.getId(), order.getOrderNo(), order.getGoodsId());
                count++;
            } catch (Exception e) {
                log.error("[定时任务] 预订超时释放异常: orderId={}", order.getId(), e);
            }
        }

        log.info("[定时任务] 预订超时自动释放完成，共处理{}个订单", count);
    }

    /**
     * 交易完成7天后未评价 -> 自动默认好评
     * cron: 每天凌晨2:30执行
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void autoDefaultGoodReviews() {
        log.info("[定时任务] 开始处理7天未评价订单，自动补默认好评");

        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        List<TradeOrder> completedOrders = tradeOrderMapper.selectList(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, 1)
                        .isNotNull(TradeOrder::getTradeTime)
                        .lt(TradeOrder::getTradeTime, deadline)
        );

        if (completedOrders.isEmpty()) {
            log.info("[定时任务] 没有需要补默认好评的订单");
            return;
        }

        int count = 0;
        for (TradeOrder order : completedOrders) {
            try {
                // 买家是否已评价
                Long buyerRated = evaluationMapper.selectCount(
                        new LambdaQueryWrapper<Evaluation>()
                                .eq(Evaluation::getOrderId, order.getId())
                                .eq(Evaluation::getEvaluatorId, order.getBuyerId())
                );
                if (buyerRated == 0) {
                    insertDefaultReview(order, order.getBuyerId(), order.getSellerId(), 1);
                    count++;
                }

                // 卖家是否已评价
                Long sellerRated = evaluationMapper.selectCount(
                        new LambdaQueryWrapper<Evaluation>()
                                .eq(Evaluation::getOrderId, order.getId())
                                .eq(Evaluation::getEvaluatorId, order.getSellerId())
                );
                if (sellerRated == 0) {
                    insertDefaultReview(order, order.getSellerId(), order.getBuyerId(), 2);
                    count++;
                }
            } catch (Exception e) {
                log.error("[定时任务] 补默认好评异常: orderId={}", order.getId(), e);
            }
        }

        log.info("[定时任务] 自动默认好评完成，共补{}条", count);
    }

    /**
     * 插入默认好评（5星）
     */
    private void insertDefaultReview(TradeOrder order, Long evaluatorId, Long evaluateeId, Integer role) {
        Evaluation eval = new Evaluation();
        eval.setOrderId(order.getId());
        eval.setGoodsId(order.getGoodsId());
        eval.setEvaluatorId(evaluatorId);
        eval.setEvaluateeId(evaluateeId);
        eval.setEvaluatorRole(role);
        eval.setScore(5);
        eval.setContent("系统默认好评");
        eval.setIsAnonymous(0);
        eval.setStatus(1);
        evaluationMapper.insert(eval);

        // 默认好评同样触发信用分奖励（与人工好评一致）
        try {
            sysUserService.updateCreditScore(
                    evaluateeId, goodReviewBonus, "获得系统默认好评", order.getId(), 0L
            );
        } catch (Exception e) {
            log.error("[定时任务] 默认好评信用分奖励失败: evaluateeId={}, orderId={}", evaluateeId, order.getId(), e);
        }

        log.info("[定时任务] 已补默认好评: orderId={}, evaluatorId={}, role={}", order.getId(), evaluatorId, role);
    }
}
