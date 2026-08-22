package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 后台仪表盘统计VO
 */
@Data
public class AdminDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 累计统计 ==========

    /** 累计用户数 */
    private Long totalUsers;

    /** 累计商品数（未逻辑删除） */
    private Long totalGoods;

    /** 累计订单数 */
    private Long totalOrders;

    /** 累计成交数 */
    private Long totalTrades;

    // ========== 今日统计 ==========

    /** 今日新增用户数 */
    private Long todayNewUsers;

    /** 今日新增商品数 */
    private Long todayNewGoods;

    /** 今日成交数 */
    private Long todayTradedGoods;

    /** 今日取消订单数 */
    private Long todayCancelledOrders;

    // ========== 待处理事项 ==========

    /** 待处理举报数 */
    private Long pendingReports;

    /** 在售商品数 */
    private Long onSaleGoods;

    /** 待交易订单数 */
    private Long pendingOrders;
}
