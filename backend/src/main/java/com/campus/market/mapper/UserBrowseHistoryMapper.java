package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户浏览历史 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 *
 * 自定义方法：
 * - insertOrUpdate: 利用 MySQL INSERT ON DUPLICATE KEY UPDATE 实现浏览记录去重+时间更新
 */
@Mapper
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory> {

    /**
     * 插入或更新浏览记录
     * 利用 user_browse_history 表的 UNIQUE KEY (user_id, goods_id)
     * 如果已存在记录，则更新 create_time 为当前时间
     *
     * @param userId  用户ID
     * @param goodsId 商品ID
     * @return 影响行数（1=新增，2=更新）
     */
    @Insert("INSERT INTO user_browse_history (user_id, goods_id, create_time) " +
            "VALUES (#{userId}, #{goodsId}, NOW()) " +
            "ON DUPLICATE KEY UPDATE create_time = NOW()")
    int insertOrUpdate(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
