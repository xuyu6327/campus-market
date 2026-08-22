package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户收到的评价 VO（含好评率统计）
 * 用于用户主页和商品详情页的"卖家口碑"展示
 */
@Data
public class UserReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 好评率（百分比，0-100） */
    private Integer goodRate;

    /** 总评价数 */
    private Long totalCount;

    /** 当前页评价列表 */
    private List<ReviewVO> records;
}
