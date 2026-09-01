package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 交易订单 Mapper
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /** 近30天完成交易趋势（管理端仪表盘） */
    @Select("SELECT DATE_FORMAT(trade_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM trade_order WHERE status = 1 AND trade_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY date ORDER BY date")
    List<Map<String, Object>> selectTradeTrend();

    /** 订单转化漏斗（总预订/已完成/已评价，管理端仪表盘） */
    @Select("SELECT (SELECT COUNT(*) FROM trade_order) AS total, " +
            "(SELECT COUNT(*) FROM trade_order WHERE status = 1) AS done, " +
            "(SELECT COUNT(DISTINCT order_id) FROM evaluation WHERE status = 1) AS reviewed")
    Map<String, Object> selectOrderFunnel();
}
