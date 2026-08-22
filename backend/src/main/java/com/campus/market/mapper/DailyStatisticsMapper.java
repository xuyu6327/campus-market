package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.DailyStatistics;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日统计 Mapper
 */
@Mapper
public interface DailyStatisticsMapper extends BaseMapper<DailyStatistics> {
}
