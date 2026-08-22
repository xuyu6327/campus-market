package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
