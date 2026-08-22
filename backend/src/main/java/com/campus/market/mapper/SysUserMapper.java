package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 * 复杂查询通过 XML 或注解实现
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
