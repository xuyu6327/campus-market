package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 商品浏览日统计实体
 * 对应 goods_daily_view 表（热门排序时间衰减用）
 * goods_id + stat_date 唯一约束，当日浏览 +1 用 ON DUPLICATE KEY UPDATE
 */
@Data
@TableName("goods_daily_view")
public class GoodsDailyView {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long goodsId;

    /** 统计日期 */
    private LocalDate statDate;

    /** 当日浏览量 */
    private Integer viewCount;
}
