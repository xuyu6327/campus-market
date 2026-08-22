package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.GoodsDailyView;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 商品浏览日统计 Mapper
 */
public interface GoodsDailyViewMapper extends BaseMapper<GoodsDailyView> {

    /**
     * 当日浏览量 +1（不存在则插入，存在则自增，基于 goods_id+stat_date 唯一索引）
     */
    @Insert("INSERT INTO goods_daily_view(goods_id, stat_date, view_count) " +
            "VALUES(#{goodsId}, CURDATE(), 1) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1")
    int addDailyView(@Param("goodsId") Long goodsId);
}
