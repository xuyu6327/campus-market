package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.CreateOrderDTO;
import com.campus.market.vo.OrderVO;

/**
 * 交易订单服务接口
 *
 * 功能列表：
 * 1. 预订商品（买家下单，需登录）
 * 2. 买家取消订单（扣信用分 -3）
 * 3. 卖家取消订单（不扣分）
 * 4. 卖家确认交易完成（信用分 +2）
 * 5. 买家提交"联系不上卖家"（24h后自动取消）
 * 6. 我买到的订单（分页）
 * 7. 我卖出的订单（分页）
 * 8. 订单详情
 *
 * 状态流转：
 *   0(待交易) --买家取消--> 2(买家取消)  [买家扣-3信用分]
 *   0(待交易) --卖家取消--> 3(卖家取消)  [不扣分]
 *   0(待交易) --卖家确认--> 1(已完成)   [卖家+2信用分]
 *   0(待交易) --联系不上24h--> 4(超时自动取消)
 */
public interface OrderService {

    /**
     * 预订商品（买家下单）
     * - 校验商品在售、不能买自己的商品
     * - 冻结买家手机号快照（AES-GCM加密）
     * - 冻结卖家QQ/微信快照
     * - 商品状态 1(在售) -> 2(预订中)
     * @return 订单ID
     */
    Long createOrder(CreateOrderDTO dto);

    /**
     * 买家取消订单
     * - 仅状态=0(待交易)可取消
     * - 买家信用分 -3
     * - 商品状态 2(预订中) -> 1(在售)
     */
    void buyerCancelOrder(Long orderId);

    /**
     * 卖家取消订单
     * - 仅状态=0(待交易)可取消
     * - 不扣任何方信用分
     * - 商品状态 2(预订中) -> 1(在售)
     */
    void sellerCancelOrder(Long orderId);

    /**
     * 卖家确认交易完成
     * - 仅状态=0(待交易)可确认
     * - 卖家信用分 +2
     * - 商品状态 2(预订中) -> 3(已售出)
     */
    void sellerConfirmTrade(Long orderId);

    /**
     * 买家提交"联系不上卖家"
     * - 仅状态=0(待交易)可提交
     * - 同一订单仅可提交一次（幂等）
     * - 设置 contactFailAt = 当前时间
     * - 24h后卖家未响应则定时任务自动取消
     */
    void reportContactFail(Long orderId);

    /**
     * 我买到的订单列表
     * @param status 订单状态筛选（可为null=全部）
     * @param keyword 商品标题关键词（可为null=不过滤）
     */
    Page<OrderVO> getMyBuyOrders(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /**
     * 我卖出的订单列表
     * @param status 订单状态筛选（可为null=全部）
     * @param keyword 商品标题关键词（可为null=不过滤）
     */
    Page<OrderVO> getMySellOrders(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /**
     * 订单详情
     * - 仅买家或卖家可查看
     * - 买家看到卖家联系方式（QQ/微信）
     * - 卖家看到买家手机号（解密明文）
     */
    OrderVO getOrderDetail(Long orderId);
}
