package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.CreditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信用分变更日志 Mapper 接口
 * 注意：此表只 INSERT，不 UPDATE / DELETE
 */
@Mapper
public interface CreditLogMapper extends BaseMapper<CreditLog> {
}
