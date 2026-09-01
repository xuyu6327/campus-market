package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 * 复杂查询通过 XML 或注解实现
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 近30天用户注册趋势（管理端仪表盘） */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM sys_user WHERE deleted = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY date ORDER BY date")
    List<Map<String, Object>> selectRegisterTrend();

    /** 信用分分布（分层统计，管理端仪表盘） */
    @Select("SELECT CASE WHEN credit_score < 10 THEN '0-9(冻结)' WHEN credit_score <= 60 THEN '10-60' " +
            "WHEN credit_score <= 80 THEN '61-80' ELSE '81-100' END AS name, COUNT(*) AS count " +
            "FROM sys_user WHERE deleted = 0 GROUP BY name ORDER BY MIN(credit_score)")
    List<Map<String, Object>> selectCreditDist();
}
