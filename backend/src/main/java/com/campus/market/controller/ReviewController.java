package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.common.Result;
import com.campus.market.dto.CreateReviewDTO;
import com.campus.market.service.ReviewService;
import com.campus.market.vo.ReviewVO;
import com.campus.market.vo.UserReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 评价控制器
 *
 * 接口列表：
 * - POST   /review              创建评价（需登录）
 * - GET    /review/goods/{id}   商品评价列表（公开）
 * - GET    /review/sent         我发出的评价（需登录）
 * - GET    /review/received     我收到的评价（需登录）
 */
@RestController
@RequestMapping("/review")
@Tag(name = "评价管理", description = "评价的创建与查询")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    @Operation(summary = "创建评价", description = "交易完成后，买家/卖家可互相评价，同一订单每人仅可评价一次")
    public Result<Long> createReview(@Valid @RequestBody CreateReviewDTO dto) {
        Long id = reviewService.createReview(dto);
        return Result.success(id);
    }

    @GetMapping("/goods/{goodsId}")
    @Operation(summary = "商品评价列表", description = "查看某商品的所有评价（公开接口，匿名评价脱敏展示）")
    public Result<Page<ReviewVO>> getGoodsReviews(
            @PathVariable Long goodsId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ReviewVO> page = reviewService.getGoodsReviews(goodsId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "用户收到的评价", description = "查看指定用户收到的评价（含好评率），用于用户主页和卖家口碑展示")
    public Result<UserReviewVO> getUserReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        UserReviewVO vo = reviewService.getUserReviews(id, pageNum, pageSize);
        return Result.success(vo);
    }

    @GetMapping("/sent")
    @Operation(summary = "我发出的评价", description = "查看当前用户发出的所有评价")
    public Result<Page<ReviewVO>> getMySentReviews(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ReviewVO> page = reviewService.getMySentReviews(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/received")
    @Operation(summary = "我收到的评价", description = "查看当前用户收到的所有评价")
    public Result<Page<ReviewVO>> getMyReceivedReviews(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ReviewVO> page = reviewService.getMyReceivedReviews(pageNum, pageSize);
        return Result.success(page);
    }
}
