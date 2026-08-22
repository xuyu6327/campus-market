package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.CreateOrderDTO;
import com.campus.market.service.OrderService;
import com.campus.market.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 交易订单控制器
 *
 * 接口列表：
 * - POST /order                    预订商品（买家下单）
 * - PUT  /order/{id}/buyer-cancel  买家取消订单
 * - PUT  /order/{id}/seller-cancel 卖家取消订单
 * - PUT  /order/{id}/confirm       卖家确认交易完成
 * - POST /order/{id}/contact-fail  买家提交"联系不上卖家"
 * - GET  /order/buy                我买到的订单
 * - GET  /order/sell               我卖出的订单
 * - GET  /order/{id}               订单详情
 *
 * 所有接口均需登录（JWT拦截器默认拦截 /** ）
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "交易订单模块", description = "预订商品、取消订单、确认交易、联系不上卖家、订单列表、订单详情")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ================== 预订商品 ==================

    @PostMapping
    @Operation(summary = "预订商品", description = "买家下单预订商品，商品状态变为\"预订中\"，冻结双方联系方式快照")
    public Result<Long> createOrder(@Validated @RequestBody CreateOrderDTO dto) {
        Long orderId = orderService.createOrder(dto);
        return Result.success("预订成功", orderId);
    }

    // ================== 买家取消订单 ==================

    @PutMapping("/{id}/buyer-cancel")
    @Operation(summary = "买家取消订单", description = "买家取消预订，扣 -3 信用分，商品恢复\"在售\"状态")
    public Result<Void> buyerCancelOrder(@PathVariable Long id) {
        orderService.buyerCancelOrder(id);
        return Result.success("取消成功", null);
    }

    // ================== 卖家取消订单 ==================

    @PutMapping("/{id}/seller-cancel")
    @Operation(summary = "卖家取消订单", description = "卖家取消交易，不扣分，商品恢复\"在售\"状态")
    public Result<Void> sellerCancelOrder(@PathVariable Long id) {
        orderService.sellerCancelOrder(id);
        return Result.success("取消成功", null);
    }

    // ================== 卖家确认交易完成 ==================

    @PutMapping("/{id}/confirm")
    @Operation(summary = "卖家确认交易完成", description = "卖家确认线下交易已完成，卖家 +2 信用分，商品状态变为\"已售出\"")
    public Result<Void> sellerConfirmTrade(@PathVariable Long id) {
        orderService.sellerConfirmTrade(id);
        return Result.success("交易完成", null);
    }

    // ================== 联系不上卖家 ==================

    @PostMapping("/{id}/contact-fail")
    @Operation(summary = "提交\"联系不上卖家\"", description = "买家提交申请，24小时后卖家未响应则系统自动取消订单")
    public Result<Void> reportContactFail(@PathVariable Long id) {
        orderService.reportContactFail(id);
        return Result.success("已提交，等待卖家响应", null);
    }

    // ================== 我买到的订单 ==================

    @GetMapping("/buy")
    @Operation(summary = "我买到的订单", description = "分页获取当前用户作为买家的订单列表")
    public Result<Page<OrderVO>> getMyBuyOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<OrderVO> page = orderService.getMyBuyOrders(pageNum, pageSize, status, keyword);
        return Result.success(page);
    }

    // ================== 我卖出的订单 ==================

    @GetMapping("/sell")
    @Operation(summary = "我卖出的订单", description = "分页获取当前用户作为卖家的订单列表")
    public Result<Page<OrderVO>> getMySellOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<OrderVO> page = orderService.getMySellOrders(pageNum, pageSize, status, keyword);
        return Result.success(page);
    }

    // ================== 订单详情 ==================

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "查看订单详情，买家可看卖家联系方式，卖家可看买家手机号")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        OrderVO vo = orderService.getOrderDetail(id);
        return Result.success(vo);
    }
}
