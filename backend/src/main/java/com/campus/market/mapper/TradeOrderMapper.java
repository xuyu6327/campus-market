package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易订单 Mapper
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {
}
