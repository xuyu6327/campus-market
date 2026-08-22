package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.GoodsQueryDTO;
import com.campus.market.dto.PublishGoodsDTO;
import com.campus.market.vo.CategoryVO;
import com.campus.market.vo.GoodsDetailVO;
import com.campus.market.vo.GoodsListVO;

import java.util.List;

/**
 * 商品服务接口
 *
 * 功能列表：
 * 1. 商品分类列表（公开）
 * 2. 发布商品（需登录）
 * 3. 商品列表（分页+筛选+排序，公开）
 * 4. 商品搜索（公开）
 * 5. 商品详情（公开，登录用户记录浏览历史）
 * 6. 收藏/取消收藏（需登录）
 * 7. 我的收藏列表（需登录）
 * 8. 浏览历史（需登录）
 * 9. 上架/下架（需登录，仅卖家）
 * 10. 我的发布（需登录）
 */
public interface GoodsService {

    /**
     * 获取启用的商品分类列表
     */
    List<CategoryVO> getCategoryList();

    /**
     * 发布商品
     * @return 商品ID
     */
    Long publishGoods(PublishGoodsDTO dto);

    /**
     * 商品列表（分页+筛选+排序）
     */
    Page<GoodsListVO> getGoodsList(GoodsQueryDTO query);

    /**
     * 商品搜索
     */
    Page<GoodsListVO> searchGoods(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 指定用户在售商品（用于用户主页）
     */
    Page<GoodsListVO> getGoodsByUser(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 商品详情
     * 如果用户已登录，记录浏览历史并标记收藏状态
     */
    GoodsDetailVO getGoodsDetail(Long goodsId);

    /**
     * 收藏商品
     */
    void favoriteGoods(Long goodsId);

    /**
     * 取消收藏
     */
    void unfavoriteGoods(Long goodsId);

    /**
     * 我的收藏列表
     */
    Page<GoodsListVO> getMyFavorites(Integer pageNum, Integer pageSize);

    /**
     * 浏览历史
     */
    Page<GoodsListVO> getBrowseHistory(Integer pageNum, Integer pageSize);

    /**
     * 重新上架（更新 last_relisted_at 为当前时间）
     * 管理员强制下架的商品（takedown_by=1）不允许直接上架，需编辑提交审核
     */
    void relistGoods(Long goodsId);

    /**
     * 编辑商品并提交重新上架审核
     * 仅限被管理员强制下架（status=0 且 takedown_by=1）或待审核（status=4）的商品
     * 提交后 status 置为 4（待审核），通知管理员
     */
    void updateGoodsAndApplyReview(Long goodsId, PublishGoodsDTO dto);

    /**
     * 下架
     */
    void takedownGoods(Long goodsId);

    /**
     * 我的发布列表
     */
    Page<GoodsListVO> getMyGoods(Integer pageNum, Integer pageSize, Integer status);
}
