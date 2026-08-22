package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.GoodsQueryDTO;
import com.campus.market.dto.PublishGoodsDTO;
import com.campus.market.service.GoodsService;
import com.campus.market.vo.CategoryVO;
import com.campus.market.vo.GoodsDetailVO;
import com.campus.market.vo.GoodsListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 *
 * 接口列表：
 * - GET  /goods/category          获取分类列表（公开）
 * - POST /goods                   发布商品（需登录）
 * - GET  /goods/list              商品列表（公开）
 * - GET  /goods/search            商品搜索（公开）
 * - GET  /goods/detail/{id}       商品详情（公开）
 * - POST /goods/{id}/favorite     收藏商品（需登录）
 * - DELETE /goods/{id}/favorite   取消收藏（需登录）
 * - GET  /goods/favorites         我的收藏（需登录）
 * - GET  /goods/history           浏览历史（需登录）
 * - PUT  /goods/{id}/relist       重新上架（需登录）
 * - PUT  /goods/{id}/takedown     下架（需登录）
 * - GET  /goods/my                我的发布（需登录）
 */
@Slf4j
@RestController
@RequestMapping("/goods")
@Tag(name = "商品模块", description = "商品分类、发布、列表、搜索、详情、收藏、浏览历史、上下架")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    // ================== 商品分类 ==================

    @GetMapping("/category")
    @Operation(summary = "获取商品分类列表", description = "返回所有启用的分类，按排序字段排列")
    public Result<List<CategoryVO>> getCategoryList() {
        List<CategoryVO> list = goodsService.getCategoryList();
        return Result.success(list);
    }

    // ================== 发布商品 ==================

    @PostMapping
    @Operation(summary = "发布商品", description = "卖家发布二手商品，图片URL列表+联系方式")
    public Result<Long> publishGoods(@Validated @RequestBody PublishGoodsDTO dto) {
        Long goodsId = goodsService.publishGoods(dto);
        return Result.success("发布成功", goodsId);
    }

    // ================== 编辑商品并申请重新上架 ==================

    @PutMapping("/{id}")
    @Operation(summary = "编辑商品并申请上架", description = "被管理员强制下架的商品，卖家修改后提交审核（进入待审核状态，管理员审核通过后重新上架）")
    public Result<Void> updateGoodsAndApplyReview(@PathVariable Long id, @Validated @RequestBody PublishGoodsDTO dto) {
        goodsService.updateGoodsAndApplyReview(id, dto);
        return Result.success("已提交审核，请等待管理员审核", null);
    }

    // ================== 商品列表 ==================

    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "分页+筛选+排序，支持分类/价格/成色/关键词筛选")
    public Result<Page<GoodsListVO>> getGoodsList(GoodsQueryDTO query) {
        Page<GoodsListVO> page = goodsService.getGoodsList(query);
        return Result.success(page);
    }

    // ================== 商品搜索 ==================

    @GetMapping("/search")
    @Operation(summary = "商品搜索", description = "按关键词搜索商品标题")
    public Result<Page<GoodsListVO>> searchGoods(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<GoodsListVO> page = goodsService.searchGoods(keyword, pageNum, pageSize);
        return Result.success(page);
    }

    // ================== 用户在售商品 ==================

    @GetMapping("/user/{id}")
    @Operation(summary = "用户在售商品", description = "查看指定用户当前在售的商品列表（用于用户主页）")
    public Result<Page<GoodsListVO>> getGoodsByUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<GoodsListVO> page = goodsService.getGoodsByUser(id, pageNum, pageSize);
        return Result.success(page);
    }

    // ================== 商品详情 ==================

    @GetMapping("/detail/{id}")
    @Operation(summary = "商品详情", description = "查看商品详情，登录用户自动记录浏览历史")
    public Result<GoodsDetailVO> getGoodsDetail(@PathVariable Long id) {
        GoodsDetailVO vo = goodsService.getGoodsDetail(id);
        return Result.success(vo);
    }

    // ================== 收藏 ==================

    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏商品", description = "收藏指定商品，不能收藏自己的商品")
    public Result<Void> favoriteGoods(@PathVariable Long id) {
        goodsService.favoriteGoods(id);
        return Result.success("收藏成功", null);
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏", description = "取消收藏指定商品")
    public Result<Void> unfavoriteGoods(@PathVariable Long id) {
        goodsService.unfavoriteGoods(id);
        return Result.success("取消收藏成功", null);
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的收藏列表", description = "分页获取当前用户收藏的商品列表")
    public Result<Page<GoodsListVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<GoodsListVO> page = goodsService.getMyFavorites(pageNum, pageSize);
        return Result.success(page);
    }

    // ================== 浏览历史 ==================

    @GetMapping("/history")
    @Operation(summary = "浏览历史", description = "分页获取当前用户的浏览历史")
    public Result<Page<GoodsListVO>> getBrowseHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<GoodsListVO> page = goodsService.getBrowseHistory(pageNum, pageSize);
        return Result.success(page);
    }

    // ================== 上架/下架 ==================

    @PutMapping("/{id}/relist")
    @Operation(summary = "重新上架", description = "卖家重新上架商品，刷新上架时间（30天自动下架重新计时）")
    public Result<Void> relistGoods(@PathVariable Long id) {
        goodsService.relistGoods(id);
        return Result.success("上架成功", null);
    }

    @PutMapping("/{id}/takedown")
    @Operation(summary = "下架商品", description = "卖家主动下架商品")
    public Result<Void> takedownGoods(@PathVariable Long id) {
        goodsService.takedownGoods(id);
        return Result.success("下架成功", null);
    }

    // ================== 我的发布 ==================

    @GetMapping("/my")
    @Operation(summary = "我的发布列表", description = "分页获取当前用户发布的商品，可按状态筛选")
    public Result<Page<GoodsListVO>> getMyGoods(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<GoodsListVO> page = goodsService.getMyGoods(pageNum, pageSize, status);
        return Result.success(page);
    }
}
