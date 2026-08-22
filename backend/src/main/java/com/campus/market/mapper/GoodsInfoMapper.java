package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.GoodsInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品信息 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 * 复杂查询（多表关联）通过 XML 或注解实现
 */
@Mapper
public interface GoodsInfoMapper extends BaseMapper<GoodsInfo> {
}
