package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 评价 Mapper
 */
@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {

    /** 评价分布（好评/中评/差评，管理端仪表盘） */
    @Select("SELECT CASE WHEN score <= 2 THEN '差评' WHEN score = 3 THEN '中评' ELSE '好评' END AS name, " +
            "COUNT(*) AS count FROM evaluation WHERE status = 1 " +
            "GROUP BY name ORDER BY MIN(score)")
    List<Map<String, Object>> selectScoreDist();

    /** 已评价订单数（去重，转化漏斗用） */
    @Select("SELECT COUNT(DISTINCT order_id) AS count FROM evaluation WHERE status = 1")
    Long selectReviewedOrderCount();
}
