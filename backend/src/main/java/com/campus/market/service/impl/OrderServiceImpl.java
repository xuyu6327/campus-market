package com.campus.market.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.CreateOrderDTO;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.SysUser;
import com.campus.market.entity.TradeOrder;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.SysUserMapper;
import com.campus.market.mapper.TradeOrderMapper;
import com.campus.market.service.NotificationService;
import com.campus.market.service.OrderService;
import com.campus.market.service.SysUserService;
import com.campus.market.util.CryptoUtils;
import com.campus.market.util.PageUtil;
import com.campus.market.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 交易订单服务实现类
 *
 * 核心逻辑：
 * 1. 预订商品：冻结联系方式快照（买家选填手机号AES-GCM加密/QQ/微信明文，卖家QQ/微信明文），商品状态 1->2
 * 2. 买家取消：扣 -3 信用分，商品状态 2->1
 * 3. 卖家取消：不扣分，商品状态 2->1
 * 4. 卖家确认：卖家 +2 信用分，商品状态 2->3(已售出)
 * 5. 联系不上卖家：幂等提交，设置 contactFailAt，24h后定时任务自动取消
 * 6. 乐观锁：订单和商品更新均使用 @Version
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private com.campus.market.mapper.EvaluationMapper evaluationMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private NotificationService notificationService;

    @Value("${campus.market.credit.buyer-cancel}")
    private Integer buyerCancelPenalty;

    @Value("${campus.market.credit.seller-cancel}")
    private Integer sellerCancelPenalty;

    @Value("${campus.market.credit.trade-complete}")
    private Integer tradeCompleteBonus;

    /** 订单状态描述 */
    private static final String[] ORDER_STATUS_DESCS = {
            "待交易", "已完成", "买家取消", "卖家取消", "超时自动取消"
    };

    // ================== 预订商品（下单） ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderDTO dto) {
        Long buyerId = JwtInterceptor.getCurrentUserId();
        log.info("[预订商品] buyerId={}, goodsId={}", buyerId, dto.getGoodsId());

        // 1. 查询商品
        GoodsInfo goods = goodsInfoMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (goods.getStatus() != 1) {
            throw new BizException(400, "商品当前状态不支持预订（仅\"在售\"状态可预订）");
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new BizException(400, "不能预订自己的商品");
        }

        // 2. 查询卖家信息（冻结联系方式快照）
        SysUser seller = sysUserMapper.selectById(goods.getSellerId());
        if (seller == null) {
            throw new BizException(404, "卖家用户不存在");
        }

        // 3. 买家联系方式：选填（手机/微信/QQ），至少填一种，仅该订单卖家可见
        String buyerPhone = trimToNull(dto.getBuyerPhone());
        String buyerQq = trimToNull(dto.getBuyerQq());
        String buyerWechat = trimToNull(dto.getBuyerWechat());
        if (buyerPhone == null && buyerQq == null && buyerWechat == null) {
            throw new BizException(400, "请至少填写一种联系方式（手机/微信/QQ），方便卖家联系您完成交易");
        }
        if (buyerPhone != null && !buyerPhone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "手机号格式不正确");
        }
        if (buyerQq != null && !buyerQq.matches("^\\d{5,15}$")) {
            throw new BizException(400, "QQ号格式不正确（5-15位数字）");
        }
        if (buyerWechat != null && !buyerWechat.matches("^[a-zA-Z0-9_-]{6,20}$")) {
            throw new BizException(400, "微信号格式不正确（6-20位字母/数字/下划线）");
        }

        // 5. 构建订单
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setGoodsId(goods.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        // 买家联系方式快照（手机号加密，QQ/微信明文；买家未填的字段为 null）
        order.setBuyerPhone(buyerPhone != null ? cryptoUtils.encryptPhone(buyerPhone) : null);
        order.setBuyerQq(buyerQq);
        order.setBuyerWechat(buyerWechat);
        // 卖家联系方式快照
        order.setSellerQq(seller.getQq() != null ? seller.getQq() : goods.getContactQq());
        order.setSellerWechat(seller.getWechat() != null ? seller.getWechat() : goods.getContactWechat());
        order.setStatus(0); // 待交易

        // 6. 插入订单
        tradeOrderMapper.insert(order);
        log.info("[订单创建成功] orderId={}, orderNo={}, goodsId={}", order.getId(), order.getOrderNo(), goods.getId());

        // 7. 商品状态 1(在售) -> 2(预订中)
        goods.setStatus(2);
        int rows = goodsInfoMapper.updateById(goods);
        if (rows == 0) {
            throw new BizException(409, "商品状态更新失败，请刷新后重试（可能已被其他人预订）");
        }

        // 8. 发送预订通知给卖家
        notificationService.sendNotification(
                goods.getSellerId(), 1,
                "新订单提醒",
                "您的商品「" + goods.getTitle() + "」已被预订，请尽快联系买家完成交易",
                order.getId()
        );

        return order.getId();
    }

    // ================== 买家取消订单 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buyerCancelOrder(Long orderId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[买家取消订单] userId={}, orderId={}", userId, orderId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BizException(403, "只有买家可以执行此操作");
        }
        if (order.getStatus() != 0) {
            throw new BizException(400, "订单当前状态不支持取消（仅\"待交易\"状态可取消）");
        }

        // 1. 更新订单状态为"买家取消"
        order.setStatus(2);
        int rows = tradeOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        // 2. 商品状态 2(预订中) -> 1(在售)
        restoreGoodsToOnSale(order.getGoodsId());

        // 3. 买家信用分 -3
        sysUserService.updateCreditScore(
                userId, buyerCancelPenalty,
                "买家取消订单", orderId, 0L
        );

        // 4. 发送取消通知给卖家
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        notificationService.sendNotification(
                order.getSellerId(), 2,
                "订单已取消",
                "买家取消了订单「" + (goods != null ? goods.getTitle() : "") + "」，商品已恢复在售",
                orderId
        );

        log.info("[买家取消成功] orderId={}, 买家信用分{}", orderId, buyerCancelPenalty);
    }

    // ================== 卖家取消订单 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerCancelOrder(Long orderId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[卖家取消订单] userId={}, orderId={}", userId, orderId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BizException(403, "只有卖家可以执行此操作");
        }
        if (order.getStatus() != 0) {
            throw new BizException(400, "订单当前状态不支持取消（仅\"待交易\"状态可取消）");
        }

        // 1. 更新订单状态为"卖家取消"
        order.setStatus(3);
        int rows = tradeOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        // 2. 商品状态 2(预订中) -> 1(在售)
        restoreGoodsToOnSale(order.getGoodsId());

        // 3. 卖家不扣分（sellerCancelPenalty = 0，不调用信用分变更）

        // 4. 发送取消通知给买家
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        notificationService.sendNotification(
                order.getBuyerId(), 2,
                "订单已取消",
                "卖家取消了订单「" + (goods != null ? goods.getTitle() : "") + "」，商品已恢复在售",
                orderId
        );

        log.info("[卖家取消成功] orderId={}", orderId);
    }

    // ================== 卖家确认交易完成 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerConfirmTrade(Long orderId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[确认交易] userId={}, orderId={}", userId, orderId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BizException(403, "只有该订单的买家或卖家可以确认交易");
        }
        if (order.getStatus() != 0) {
            throw new BizException(400, "订单当前状态不支持确认（仅\"待交易\"状态可确认）");
        }

        // 1. 更新订单状态为"已完成"
        order.setStatus(1);
        order.setTradeTime(LocalDateTime.now());
        int rows = tradeOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        // 2. 商品状态 2(预订中) -> 3(已售出)
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        if (goods != null) {
            goods.setStatus(3);
            goodsInfoMapper.updateById(goods);
        }

        // 3. 卖家信用分 +2（固定给卖家，与谁确认无关）
        sysUserService.updateCreditScore(
                order.getSellerId(), tradeCompleteBonus,
                "完成交易", orderId, 0L
        );

        // 4. 发送交易完成通知给买家
        notificationService.sendNotification(
                order.getBuyerId(), 3,
                "交易已完成",
                "您预订的「" + (goods != null ? goods.getTitle() : "") + "」交易已确认完成，请互相评价",
                orderId
        );

        log.info("[交易完成] orderId={}, 卖家信用分+{}", orderId, tradeCompleteBonus);
    }

    // ================== 联系不上卖家 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportContactFail(Long orderId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[联系不上卖家] userId={}, orderId={}", userId, orderId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BizException(403, "只有买家可以执行此操作");
        }
        if (order.getStatus() != 0) {
            throw new BizException(400, "订单当前状态不支持此操作（仅\"待交易\"状态可提交）");
        }

        // 幂等校验：同一订单仅可提交一次
        if (order.getContactFailAt() != null) {
            throw new BizException(400, "已提交过\"联系不上卖家\"申请，请等待卖家响应或系统自动处理");
        }

        // 设置 contactFailAt 为当前时间
        order.setContactFailAt(LocalDateTime.now());
        int rows = tradeOrderMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        // 发送"联系不上卖家"通知
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        notificationService.sendNotification(
                order.getSellerId(), 5,
                "买家反馈联系不上您",
                "买家反馈联系不上您处理订单「" + (goods != null ? goods.getTitle() : "") + "」，请尽快回应，否则24小时后订单将自动取消",
                orderId
        );

        log.info("[联系不上卖家-已记录] orderId={}, contactFailAt={}", orderId, order.getContactFailAt());
    }

    // ================== 我买到的订单 ==================

    @Override
    public Page<OrderVO> getMyBuyOrders(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<TradeOrder> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getBuyerId, userId)
                .eq(status != null, TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getCreateTime);

        // 关键词按商品标题过滤：先查匹配的商品ID，再过滤订单（空结果直接返回空页）
        List<Long> goodsIds = matchGoodsIdsByKeyword(keyword);
        if (keyword != null && !keyword.trim().isEmpty() && goodsIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        if (goodsIds != null) {
            wrapper.in(TradeOrder::getGoodsId, goodsIds);
        }

        Page<TradeOrder> orderPage = tradeOrderMapper.selectPage(page, wrapper);
        return convertToOrderVOPage(orderPage, userId);
    }

    // ================== 我卖出的订单 ==================

    @Override
    public Page<OrderVO> getMySellOrders(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<TradeOrder> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getSellerId, userId)
                .eq(status != null, TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getCreateTime);

        // 关键词按商品标题过滤：先查匹配的商品ID，再过滤订单（空结果直接返回空页）
        List<Long> goodsIds = matchGoodsIdsByKeyword(keyword);
        if (keyword != null && !keyword.trim().isEmpty() && goodsIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        if (goodsIds != null) {
            wrapper.in(TradeOrder::getGoodsId, goodsIds);
        }

        Page<TradeOrder> orderPage = tradeOrderMapper.selectPage(page, wrapper);
        return convertToOrderVOPage(orderPage, userId);
    }

    /**
     * 按商品标题关键词匹配商品ID列表
     * @return null 表示无关键词过滤；否则返回匹配的商品ID列表（可能为空）
     */
    private List<Long> matchGoodsIdsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return goodsInfoMapper.selectList(
                new LambdaQueryWrapper<GoodsInfo>()
                        .like(GoodsInfo::getTitle, keyword.trim())
                        .select(GoodsInfo::getId)
        ).stream().map(GoodsInfo::getId).collect(Collectors.toList());
    }

    // ================== 订单详情 ==================

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Long userId = JwtInterceptor.getCurrentUserId();

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }

        // 权限校验：仅买家或卖家可查看
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BizException(403, "无权查看此订单");
        }

        return convertToOrderVO(order, userId, isBuyer, isSeller);
    }

    // ================== 工具方法 ==================

    /**
     * 商品状态恢复为在售（2->1）
     * 使用 LambdaUpdateWrapper 直接 SET，避免乐观锁冲突
     */
    private void restoreGoodsToOnSale(Long goodsId) {
        goodsInfoMapper.update(null,
                new LambdaUpdateWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getId, goodsId)
                        .eq(GoodsInfo::getStatus, 2)
                        .set(GoodsInfo::getStatus, 1)
        );
    }

    /**
     * 去空白并转为 null（空串统一视为未填写）
     */
    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 生成订单编号：yyyyMMddHHmmss + 6位UUID片段
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return timestamp + uuid;
    }

    /**
     * 将 TradeOrder Page 转换为 OrderVO Page
     */
    private Page<OrderVO> convertToOrderVOPage(Page<TradeOrder> orderPage, Long currentUserId) {
        Page<OrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderVO> voList = orderPage.getRecords().stream()
                .map(order -> {
                    boolean isBuyer = order.getBuyerId().equals(currentUserId);
                    boolean isSeller = order.getSellerId().equals(currentUserId);
                    return convertToOrderVO(order, currentUserId, isBuyer, isSeller);
                })
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    /**
     * 将 TradeOrder 转换为 OrderVO
     */
    private OrderVO convertToOrderVO(TradeOrder order, Long currentUserId, boolean isBuyer, boolean isSeller) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setGoodsId(order.getGoodsId());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getOrderStatusDesc(order.getStatus()));
        vo.setContactFailed(order.getContactFailAt() != null);
        vo.setContactFailAt(order.getContactFailAt());
        vo.setTradeTime(order.getTradeTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setIsBuyer(isBuyer);
        vo.setIsSeller(isSeller);
        // 当前用户是否已评价该订单（已完成状态用于前端按钮展示）
        Long ratedCount = evaluationMapper.selectCount(
                new LambdaQueryWrapper<com.campus.market.entity.Evaluation>()
                        .eq(com.campus.market.entity.Evaluation::getOrderId, order.getId())
                        .eq(com.campus.market.entity.Evaluation::getEvaluatorId, currentUserId)
        );
        vo.setRated(ratedCount != null && ratedCount > 0);

        // 查询商品信息
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        if (goods != null) {
            vo.setGoodsTitle(goods.getTitle());
            vo.setGoodsPrice(goods.getPrice());
            // 封面图
            List<String> images = parseImages(goods.getImages());
            if (!images.isEmpty()) {
                vo.setGoodsCoverImage(images.get(0));
            }
        }

        // 查询买家信息
        SysUser buyer = sysUserMapper.selectById(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerNickname(buyer.getNickname());
        }

        // 查询卖家信息
        SysUser seller = sysUserMapper.selectById(order.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerStatus(seller.getStatus());
        }

        // 联系方式展示规则：
        // - 买家看到卖家联系方式（QQ/微信/手机号）
        // - 卖家看到买家填写的联系方式（手机号解密明文/QQ/微信，未填则为 null）
        if (isBuyer) {
            vo.setSellerQq(order.getSellerQq());
            vo.setSellerWechat(order.getSellerWechat());
            // 卖家手机号：从商品联系方式解密（仅当卖家留了手机号）
            if (goods != null && goods.getContactPhone() != null && !goods.getContactPhone().isEmpty()) {
                try {
                    vo.setSellerPhone(cryptoUtils.decryptPhone(goods.getContactPhone()));
                } catch (Exception e) {
                    log.warn("[订单详情] 卖家手机号解密失败: orderId={}", order.getId());
                }
            }
        }
        if (isSeller) {
            // 解密买家手机号（买家填了才展示，未填为 null）
            if (order.getBuyerPhone() != null) {
                vo.setBuyerPhone(cryptoUtils.decryptPhone(order.getBuyerPhone()));
            }
            vo.setBuyerQq(order.getBuyerQq());
            vo.setBuyerWechat(order.getBuyerWechat());
        }

        return vo;
    }

    /**
     * 解析图片 JSON 为 List
     */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        return JSONUtil.toList(imagesJson, String.class);
    }

    /**
     * 获取订单状态描述
     */
    private String getOrderStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return ORDER_STATUS_DESCS[status];
    }
}
