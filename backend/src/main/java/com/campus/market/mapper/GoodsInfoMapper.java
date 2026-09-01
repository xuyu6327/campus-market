package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.GoodsInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商品信息 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 * 复杂查询（多表关联）通过 XML 或注解实现
 */
@Mapper
public interface GoodsInfoMapper extends BaseMapper<GoodsInfo> {

    // ===== 管理端仪表盘统计（图表聚合，注意手动加逻辑删除条件 deleted=0） =====

    /** 近30天商品发布趋势 */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM goods_info WHERE deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY date ORDER BY date")
    List<Map<String, Object>> selectPublishTrend();

    /** 商品分类分布（含分类名） */
    @Select("SELECT c.name AS name, COUNT(g.id) AS count FROM goods_info g " +
            "LEFT JOIN goods_category c ON g.category_id = c.id " +
            "WHERE g.deleted = 0 GROUP BY g.category_id, c.name ORDER BY count DESC")
    List<Map<String, Object>> selectCategoryDist();

    /** 商品状态分布 */
    @Select("SELECT status AS name, COUNT(*) AS count FROM goods_info " +
            "WHERE deleted = 0 GROUP BY status ORDER BY count DESC")
    List<Map<String, Object>> selectStatusDist();

    /** 商品成色分布 */
    @Select("SELECT goods_condition AS name, COUNT(*) AS count FROM goods_info " +
            "WHERE deleted = 0 GROUP BY goods_condition ORDER BY count DESC")
    List<Map<String, Object>> selectConditionDist();

    /** 热门商品 TOP10（按浏览量） */
    @Select("SELECT id, title, view_count AS viewCount, favorite_count AS favoriteCount " +
            "FROM goods_info WHERE deleted = 0 ORDER BY view_count DESC, favorite_count DESC LIMIT 10")
    List<Map<String, Object>> selectTopGoods();

    /** 活跃用户 TOP10（按发布商品数） */
    @Select("SELECT g.seller_id AS userId, u.nickname AS nickname, COUNT(*) AS count " +
            "FROM goods_info g LEFT JOIN sys_user u ON g.seller_id = u.id " +
            "WHERE g.deleted = 0 GROUP BY g.seller_id, u.nickname ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectTopUsers();

    /** 交易地点分布 */
    @Select("SELECT trade_location AS name, COUNT(*) AS count FROM goods_info " +
            "WHERE deleted = 0 AND trade_location IS NOT NULL AND trade_location != '' " +
            "GROUP BY trade_location ORDER BY count DESC")
    List<Map<String, Object>> selectLocationDist();
}
