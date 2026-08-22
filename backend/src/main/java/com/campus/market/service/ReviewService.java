package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.CreateReviewDTO;
import com.campus.market.vo.ReviewVO;
import com.campus.market.vo.UserReviewVO;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 创建评价
     * 校验：订单状态为已完成、未重复评价、评价人身份校验
     */
    Long createReview(CreateReviewDTO dto);

    /**
     * 查看商品评价列表（公开接口，只返回买家对卖家的评价）
     */
    Page<ReviewVO> getGoodsReviews(Long goodsId, Integer pageNum, Integer pageSize);

    /**
     * 查看指定用户收到的评价（含好评率，用于用户主页和卖家口碑）
     */
    UserReviewVO getUserReviews(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 我发出的评价
     */
    Page<ReviewVO> getMySentReviews(Integer pageNum, Integer pageSize);

    /**
     * 我收到的评价
     */
    Page<ReviewVO> getMyReceivedReviews(Integer pageNum, Integer pageSize);
}
