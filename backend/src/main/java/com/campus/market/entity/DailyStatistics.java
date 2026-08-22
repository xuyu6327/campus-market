package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计聚合实体
 * 对应数据库表 daily_statistics
 *
 * 用于后台仪表盘展示历史统计趋势
 */
@Data
@TableName("daily_statistics")
public class DailyStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate statDate;

    /** 新增用户数 */
    private Integer newUsers;

    /** 新增商品数 */
    private Integer newGoods;

    /** 成交商品数 */
    private Integer tradedGoods;

    /** 取消订单数 */
    private Integer cancelledOrders;

    /** 累计用户数 */
    private Integer totalUsers;

    /** 累计商品数 */
    private Integer totalGoods;

    /** 累计成交数 */
    private Integer totalTrades;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
