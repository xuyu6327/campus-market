package com.campus.market.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.GoodsQueryDTO;
import com.campus.market.dto.PublishGoodsDTO;
import com.campus.market.entity.*;
import com.campus.market.mapper.*;
import com.campus.market.service.GoodsService;
import com.campus.market.service.SensitiveWordService;
import com.campus.market.util.CryptoUtils;
import com.campus.market.util.PageUtil;
import com.campus.market.vo.CategoryVO;
import com.campus.market.vo.GoodsDetailVO;
import com.campus.market.vo.GoodsListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 *
 * 核心逻辑：
 * 1. 发布商品：图片URL转JSON存储，联系手机号AES-GCM加密
 * 2. 商品列表：支持分类/价格/成色筛选 + 多种排序，只返回在售商品
 * 3. 商品详情：浏览量+1（独立UPDATE避免乐观锁冲突），登录用户记录浏览历史
 * 4. 收藏：利用唯一索引(user_id, goods_id)防重，收藏数同步到goods_info
 * 5. 浏览历史：INSERT ON DUPLICATE KEY UPDATE，同一商品只保留最新浏览时间
 * 6. 30天自动下架：由 GoodsScheduledTask 定时执行
 */
@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private UserBrowseHistoryMapper userBrowseHistoryMapper;

    @Autowired
    private GoodsDailyViewMapper goodsDailyViewMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Autowired
    private com.campus.market.service.NotificationService notificationService;

    /** 成色描述映射（与前端 CONDITION_MAP 一致；index 6 兼容旧数据） */
    private static final String[] CONDITION_DESCS = {
            "", "全新未拆", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "故障/坏件", "故障/坏件"
    };

    /** 状态描述映射 */
    private static final String[] STATUS_DESCS = {
            "已下架", "在售", "预订中", "已售出", "待审核"
    };

    // ================== 商品分类 ==================

    @Override
    public List<CategoryVO> getCategoryList() {
        List<GoodsCategory> categories = goodsCategoryMapper.selectList(
                new LambdaQueryWrapper<GoodsCategory>()
                        .eq(GoodsCategory::getStatus, 1)
                        .orderByAsc(GoodsCategory::getSortOrder)
        );
        return categories.stream().map(cat -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(cat.getId());
            vo.setParentId(cat.getParentId());
            vo.setName(cat.getName());
            vo.setIcon(cat.getIcon());
            vo.setSortOrder(cat.getSortOrder());
            return vo;
        }).collect(Collectors.toList());
    }

    // ================== 发布商品 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishGoods(PublishGoodsDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[发布商品] userId={}, title={}", userId, dto.getTitle());

        // 0. 发布数量限制
        // 0.1 在售商品上限20件
        Long onSaleCount = goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getSellerId, userId)
                        .eq(GoodsInfo::getStatus, 1)
        );
        if (onSaleCount >= 20) {
            throw new BizException(400, "在售商品已达上限（20件），请先下架部分商品");
        }
        // 0.2 24小时内最多发布5件
        Long todayCount = goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getSellerId, userId)
                        .ge(GoodsInfo::getCreateTime, LocalDateTime.now().minusHours(24))
        );
        if (todayCount >= 5) {
            throw new BizException(400, "24小时内最多发布5件商品，请稍后再试");
        }

        // 1. 校验分类是否存在
        GoodsCategory category = goodsCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == 0) {
            throw new BizException(400, "商品分类不存在或已禁用");
        }

        // 2. 校验联系方式
        validateContactInfo(dto);

        // 2.3 成色图片数量校验（成色越差需越多图）
        int minImages;
        switch (dto.getGoodsCondition()) {
            case 3: minImages = 2; break;   // 9成新至少2张
            case 4: case 5: case 6: minImages = 3; break; // 8成新/战损/故障至少3张
            default: minImages = 1; break;  // 全新/99新至少1张
        }
        if (dto.getImages() == null || dto.getImages().size() < minImages) {
            throw new BizException(400, "该成色至少需要" + minImages + "张图片");
        }

        // 2.5 敏感词过滤（标题+描述）
        if (sensitiveWordService.containsSensitive(dto.getTitle())
                || (dto.getDescription() != null && !dto.getDescription().isEmpty()
                    && sensitiveWordService.containsSensitive(dto.getDescription()))) {
            throw new BizException(400, "商品标题或描述包含敏感词，请修改后重新提交");
        }

        // 3. 构建商品实体
        GoodsInfo goods = new GoodsInfo();
        goods.setSellerId(userId);
        goods.setTitle(dto.getTitle());
        goods.setDescription(dto.getDescription());
        goods.setCategoryId(dto.getCategoryId());
        goods.setPrice(dto.getPrice());
        goods.setOriginalPrice(dto.getOriginalPrice());
        goods.setGoodsCondition(dto.getGoodsCondition());
        // 图片列表转 JSON 存储
        goods.setImages(JSONUtil.toJsonStr(dto.getImages()));
        goods.setTradeLocation(dto.getTradeLocation());
        goods.setContactMethod(dto.getContactMethod());
        goods.setContactQq(dto.getContactQq());
        goods.setContactWechat(dto.getContactWechat());
        // 联系手机号加密存储
        if (dto.getContactPhone() != null && !dto.getContactPhone().isEmpty()) {
            goods.setContactPhone(cryptoUtils.encryptPhone(dto.getContactPhone()));
        }
        // 初始值
        goods.setStatus(1);       // 在售
        goods.setViewCount(0);
        goods.setFavoriteCount(0);
        goods.setLastRelistedAt(LocalDateTime.now());

        // 4. 插入数据库
        goodsInfoMapper.insert(goods);
        log.info("[发布商品成功] goodsId={}, sellerId={}", goods.getId(), userId);

        return goods.getId();
    }

    // ================== 商品列表 ==================

    @Override
    public Page<GoodsListVO> getGoodsList(GoodsQueryDTO query) {
        Page<GoodsInfo> page = PageUtil.of(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getStatus, query.getStatus())
                .eq(query.getCategoryId() != null, GoodsInfo::getCategoryId, query.getCategoryId())
                .ge(query.getMinPrice() != null, GoodsInfo::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, GoodsInfo::getPrice, query.getMaxPrice())
                .eq(query.getGoodsCondition() != null, GoodsInfo::getGoodsCondition, query.getGoodsCondition())
                .like(query.getKeyword() != null && !query.getKeyword().isEmpty(),
                        GoodsInfo::getTitle, query.getKeyword());

        // 排序
        switch (query.getSortBy()) {
            case "price_asc":
                wrapper.orderByAsc(GoodsInfo::getPrice);
                break;
            case "price_desc":
                wrapper.orderByDesc(GoodsInfo::getPrice);
                break;
            case "popular":
                // 多因子热门排序：近7天浏览量×1 + 收藏数×5 + 新品期(上架≤3天)×25
                // 时间衰减：用 goods_daily_view 近7天浏览量替代累计浏览量，避免老商品霸榜
                // 新品加权≈5个收藏的热度：有曝光但不霸榜
                wrapper.last("ORDER BY (" +
                        "(SELECT IFNULL(SUM(view_count),0) FROM goods_daily_view " +
                        "WHERE goods_id = goods_info.id AND stat_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)) * 1" +
                        " + favorite_count * 5" +
                        " + IF(DATEDIFF(CURDATE(), create_time) <= 3, 25, 0)" +
                        ") DESC, create_time DESC");
                break;
            case "newest":
            default:
                wrapper.orderByDesc(GoodsInfo::getCreateTime);
                break;
        }

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);

        // 转换为 VO
        return convertToGoodsListPage(goodsPage);
    }

    // ================== 商品搜索 ==================

    @Override
    public Page<GoodsListVO> searchGoods(String keyword, Integer pageNum, Integer pageSize) {
        Page<GoodsInfo> page = PageUtil.of(pageNum, pageSize);

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getStatus, 1)
                .like(GoodsInfo::getTitle, keyword)
                .orderByDesc(GoodsInfo::getCreateTime);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        return convertToGoodsListPage(goodsPage);
    }

    @Override
    public Page<GoodsListVO> getGoodsByUser(Long userId, Integer pageNum, Integer pageSize) {
        Page<GoodsInfo> page = PageUtil.of(pageNum, pageSize);

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getSellerId, userId)
                .eq(GoodsInfo::getStatus, 1) // 仅在售
                .orderByDesc(GoodsInfo::getCreateTime);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        return convertToGoodsListPage(goodsPage);
    }

    // ================== 商品详情 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsDetailVO getGoodsDetail(Long goodsId) {
        // 1. 查询商品
        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }

        // 2. 浏览量 +1（SQL 自增，避免并发读取旧值）
        goodsInfoMapper.update(null,
                new LambdaUpdateWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getId, goodsId)
                        .setSql("view_count = view_count + 1")
        );
        // 2.5 浏览日统计写入（热门排序时间衰减数据源；失败不影响浏览主流程）
        try {
            goodsDailyViewMapper.addDailyView(goodsId);
        } catch (Exception e) {
            log.warn("[浏览日统计写入失败] goodsId={}", goodsId, e);
        }

        // 3. 获取当前用户ID（可选，未登录返回 null）
        Long currentUserId = JwtInterceptor.getCurrentUserIdOrNull();

        // 4. 登录用户记录浏览历史
        if (currentUserId != null) {
            userBrowseHistoryMapper.insertOrUpdate(currentUserId, goodsId);
        }

        // 5. 查询卖家信息
        SysUser seller = sysUserMapper.selectById(goods.getSellerId());

        // 6. 查询分类名称
        GoodsCategory category = goodsCategoryMapper.selectById(goods.getCategoryId());

        // 7. 查询当前用户是否已收藏
        boolean favorited = false;
        if (currentUserId != null) {
            Long favCount = userFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<UserFavorite>()
                            .eq(UserFavorite::getUserId, currentUserId)
                            .eq(UserFavorite::getGoodsId, goodsId)
            );
            favorited = favCount > 0;
        }

        // 8. 构建 VO
        GoodsDetailVO vo = new GoodsDetailVO();
        vo.setId(goods.getId());
        vo.setTitle(goods.getTitle());
        vo.setDescription(goods.getDescription());
        vo.setPrice(goods.getPrice());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setGoodsCondition(goods.getGoodsCondition());
        vo.setConditionDesc(getConditionDesc(goods.getGoodsCondition()));
        // 解析图片 JSON
        vo.setImages(parseImages(goods.getImages()));
        vo.setTradeLocation(goods.getTradeLocation());
        vo.setCategoryId(goods.getCategoryId());
        vo.setCategoryName(category != null ? category.getName() : null);
        vo.setStatus(goods.getStatus());
        vo.setStatusDesc(getStatusDesc(goods.getStatus()));
        vo.setViewCount(goods.getViewCount() + 1);  // 加上本次浏览
        vo.setFavoriteCount(goods.getFavoriteCount());
        vo.setCreateTime(goods.getCreateTime());
        vo.setLastRelistedAt(goods.getLastRelistedAt());

        // 卖家信息
        if (seller != null) {
            vo.setSellerId(seller.getId());
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
            vo.setSellerCreditScore(seller.getCreditScore());
            vo.setSellerStatus(seller.getStatus());
        }

        // 联系方式
        vo.setContactMethod(goods.getContactMethod());
        vo.setContactMethodDesc(getContactMethodDesc(goods.getContactMethod()));
        vo.setContactQq(goods.getContactQq());
        vo.setContactWechat(goods.getContactWechat());
        // 解密并脱敏手机号
        if (goods.getContactPhone() != null && !goods.getContactPhone().isEmpty()) {
            String decrypted = cryptoUtils.decryptPhone(goods.getContactPhone());
            vo.setContactPhone(maskPhone(decrypted));
        }

        // 当前用户相关
        vo.setFavorited(favorited);
        vo.setIsOwner(currentUserId != null && currentUserId.equals(goods.getSellerId()));

        return vo;
    }

    // ================== 收藏 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteGoods(Long goodsId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[收藏商品] userId={}, goodsId={}", userId, goodsId);

        // 1. 校验商品存在且在售
        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }

        // 2. 不能收藏自己的商品
        if (goods.getSellerId().equals(userId)) {
            throw new BizException(400, "不能收藏自己的商品");
        }

        // 2.5 收藏数量上限100件
        Long favTotal = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, userId)
        );
        if (favTotal >= 100) {
            throw new BizException(400, "收藏已满（上限100件），请清理后再收藏");
        }

        // 3. 检查是否已收藏（利用唯一索引防重）
        Long existCount = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getGoodsId, goodsId)
        );
        if (existCount > 0) {
            throw new BizException(400, "已收藏过该商品");
        }

        // 4. 插入收藏记录
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(goodsId);
        userFavoriteMapper.insert(favorite);

        // 5. 商品收藏数 +1（SQL 原子自增，避免并发读-改-写丢失计数）
        goodsInfoMapper.update(null,
                new LambdaUpdateWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getId, goodsId)
                        .setSql("favorite_count = favorite_count + 1")
        );

        log.info("[收藏成功] userId={}, goodsId={}", userId, goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteGoods(Long goodsId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[取消收藏] userId={}, goodsId={}", userId, goodsId);

        // 1. 检查是否已收藏
        UserFavorite existing = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getGoodsId, goodsId)
        );
        if (existing == null) {
            throw new BizException(400, "未收藏该商品");
        }

        // 2. 删除收藏记录
        userFavoriteMapper.deleteById(existing.getId());

        // 3. 商品收藏数 -1（SQL 原子递减，GREATEST 防负）
        goodsInfoMapper.update(null,
                new LambdaUpdateWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getId, goodsId)
                        .setSql("favorite_count = GREATEST(0, favorite_count - 1)")
        );

        log.info("[取消收藏成功] userId={}, goodsId={}", userId, goodsId);
    }

    @Override
    public Page<GoodsListVO> getMyFavorites(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        // 1. 查询用户收藏的商品ID列表（分页）
        Page<UserFavorite> favPage = PageUtil.of(pageNum, pageSize);
        Page<UserFavorite> favResult = userFavoriteMapper.selectPage(favPage,
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime)
        );

        // 2. 根据商品ID列表批量查询商品信息
        List<Long> goodsIds = favResult.getRecords().stream()
                .map(UserFavorite::getGoodsId)
                .collect(Collectors.toList());
        List<GoodsListVO> voList = new ArrayList<>();
        if (!goodsIds.isEmpty()) {
            List<GoodsInfo> goodsList = goodsInfoMapper.selectBatchIds(goodsIds);
            for (GoodsInfo goods : goodsList) {
                if (goods != null && goods.getDeleted() == 0) {
                    voList.add(convertToGoodsListVO(goods));
                }
            }
        }

        // 3. 构建返回分页对象
        Page<GoodsListVO> result = new Page<>(pageNum, pageSize, favResult.getTotal());
        result.setRecords(voList);
        return result;
    }

    // ================== 浏览历史 ==================

    @Override
    public Page<GoodsListVO> getBrowseHistory(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        // 1. 查询浏览历史（分页）
        Page<UserBrowseHistory> histPage = PageUtil.of(pageNum, pageSize);
        Page<UserBrowseHistory> histResult = userBrowseHistoryMapper.selectPage(histPage,
                new LambdaQueryWrapper<UserBrowseHistory>()
                        .eq(UserBrowseHistory::getUserId, userId)
                        .orderByDesc(UserBrowseHistory::getCreateTime)
        );

        // 2. 根据商品ID列表批量查询商品信息
        List<Long> goodsIds = histResult.getRecords().stream()
                .map(UserBrowseHistory::getGoodsId)
                .collect(Collectors.toList());
        List<GoodsListVO> voList = new ArrayList<>();
        if (!goodsIds.isEmpty()) {
            List<GoodsInfo> goodsList = goodsInfoMapper.selectBatchIds(goodsIds);
            for (GoodsInfo goods : goodsList) {
                if (goods != null && goods.getDeleted() == 0) {
                    voList.add(convertToGoodsListVO(goods));
                }
            }
        }

        // 3. 构建返回分页对象
        Page<GoodsListVO> result = new Page<>(pageNum, pageSize, histResult.getTotal());
        result.setRecords(voList);
        return result;
    }

    // ================== 上架/下架 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void relistGoods(Long goodsId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[重新上架] userId={}, goodsId={}", userId, goodsId);

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new BizException(403, "只能操作自己的商品");
        }
        if (goods.getStatus() == 1) {
            throw new BizException(400, "商品已在售，无需重复上架");
        }
        if (goods.getStatus() == 3) {
            throw new BizException(400, "商品已售出，无法重新上架");
        }
        if (goods.getStatus() == 2) {
            throw new BizException(400, "商品预订中，无法重新上架");
        }
        // 管理员强制下架的商品不允许直接上架，需修改后提交审核
        if (goods.getTakedownBy() != null && goods.getTakedownBy() == 1) {
            throw new BizException(400, "商品已被管理员下架，请先修改商品并提交审核上架");
        }

        // 更新状态为在售 + 刷新上架时间
        goods.setStatus(1);
        goods.setLastRelistedAt(LocalDateTime.now());
        int rows = goodsInfoMapper.updateById(goods);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        log.info("[重新上架成功] goodsId={}", goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void takedownGoods(Long goodsId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[下架商品] userId={}, goodsId={}", userId, goodsId);

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new BizException(403, "只能操作自己的商品");
        }
        if (goods.getStatus() == 0) {
            throw new BizException(400, "商品已下架");
        }
        if (goods.getStatus() == 2) {
            throw new BizException(400, "商品预订中，无法下架");
        }
        if (goods.getStatus() == 3) {
            throw new BizException(400, "商品已售出，无法下架");
        }

        goods.setStatus(0);
        int rows = goodsInfoMapper.updateById(goods);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        log.info("[下架成功] goodsId={}", goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoodsAndApplyReview(Long goodsId, PublishGoodsDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[编辑并申请上架] userId={}, goodsId={}", userId, goodsId);

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new BizException(403, "只能操作自己的商品");
        }
        // 仅被管理员强制下架（status=0 且 takedownBy=1）或待审核（status=4）的商品可编辑提交
        boolean forceDown = goods.getStatus() == 0
                && goods.getTakedownBy() != null && goods.getTakedownBy() == 1;
        if (!forceDown && goods.getStatus() != 4) {
            throw new BizException(400, "仅被管理员下架的商品可以编辑并申请重新上架");
        }

        // 1. 校验分类是否存在且启用
        GoodsCategory category = goodsCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == 0) {
            throw new BizException(400, "商品分类不存在或已禁用");
        }

        // 2. 校验联系方式
        validateContactInfo(dto);

        // 3. 成色图片数量校验
        int minImages;
        switch (dto.getGoodsCondition()) {
            case 3: minImages = 2; break;
            case 4: case 5: case 6: minImages = 3; break;
            default: minImages = 1; break;
        }
        if (dto.getImages() == null || dto.getImages().size() < minImages) {
            throw new BizException(400, "该成色至少需要" + minImages + "张图片");
        }

        // 4. 敏感词过滤（标题+描述）
        if (sensitiveWordService.containsSensitive(dto.getTitle())
                || (dto.getDescription() != null && !dto.getDescription().isEmpty()
                    && sensitiveWordService.containsSensitive(dto.getDescription()))) {
            throw new BizException(400, "商品标题或描述包含敏感词，请修改后重新提交");
        }

        // 5. 更新商品字段（手机号未传则保留原加密值）
        goods.setTitle(dto.getTitle());
        goods.setDescription(dto.getDescription());
        goods.setCategoryId(dto.getCategoryId());
        goods.setPrice(dto.getPrice());
        goods.setOriginalPrice(dto.getOriginalPrice());
        goods.setGoodsCondition(dto.getGoodsCondition());
        goods.setImages(JSONUtil.toJsonStr(dto.getImages()));
        goods.setTradeLocation(dto.getTradeLocation());
        goods.setContactMethod(dto.getContactMethod());
        goods.setContactQq(dto.getContactQq());
        goods.setContactWechat(dto.getContactWechat());
        if (dto.getContactPhone() != null && !dto.getContactPhone().isEmpty()) {
            goods.setContactPhone(cryptoUtils.encryptPhone(dto.getContactPhone()));
        }
        goods.setStatus(4); // 待审核
        int rows = goodsInfoMapper.updateById(goods);
        if (rows == 0) {
            throw new BizException(409, "操作失败，请刷新后重试");
        }

        // 6. 通知管理员审核（管理员账号固定 id=1）
        try {
            notificationService.sendNotification(
                    1L, 7, "商品重新上架审核",
                    "卖家提交了商品「" + goods.getTitle() + "」的重新上架申请，请及时审核。",
                    goodsId
            );
        } catch (Exception e) {
            log.error("[提交审核通知失败] goodsId={}", goodsId, e);
        }

        log.info("[编辑并申请上架成功] goodsId={}", goodsId);
    }

    // ================== 我的发布 ==================

    @Override
    public Page<GoodsListVO> getMyGoods(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<GoodsInfo> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getSellerId, userId)
                .eq(status != null, GoodsInfo::getStatus, status)
                .orderByDesc(GoodsInfo::getCreateTime);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        return convertToGoodsListPage(goodsPage);
    }

    // ================== 工具方法 ==================

    /**
     * 校验联系方式信息
     */
    private void validateContactInfo(PublishGoodsDTO dto) {
        int method = dto.getContactMethod();
        switch (method) {
            case 1: // 手机
                if (dto.getContactPhone() == null || dto.getContactPhone().isEmpty()) {
                    throw new BizException(400, "联系方式为手机时，手机号不能为空");
                }
                if (!dto.getContactPhone().matches("^1[3-9]\\d{9}$")) {
                    throw new BizException(400, "手机号格式不正确");
                }
                break;
            case 2: // QQ
                if (dto.getContactQq() == null || dto.getContactQq().isEmpty()) {
                    throw new BizException(400, "联系方式为QQ时，QQ号不能为空");
                }
                break;
            case 3: // 微信
                if (dto.getContactWechat() == null || dto.getContactWechat().isEmpty()) {
                    throw new BizException(400, "联系方式为微信时，微信号不能为空");
                }
                break;
            default:
                throw new BizException(400, "联系方式类型无效");
        }
    }

    /**
     * 将 GoodsInfo Page 转换为 GoodsListVO Page
     */
    private Page<GoodsListVO> convertToGoodsListPage(Page<GoodsInfo> goodsPage) {
        Page<GoodsListVO> result = new Page<>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal());
        List<GoodsInfo> goodsList = goodsPage.getRecords();

        // 批量预查询分类和卖家，避免 N+1
        Map<Long, GoodsCategory> catMap = new HashMap<>();
        Map<Long, SysUser> sellerMap = new HashMap<>();
        if (!goodsList.isEmpty()) {
            List<Long> catIds = goodsList.stream().map(GoodsInfo::getCategoryId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<Long> sellerIds = goodsList.stream().map(GoodsInfo::getSellerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!catIds.isEmpty()) {
                goodsCategoryMapper.selectBatchIds(catIds).forEach(cat -> catMap.put(cat.getId(), cat));
            }
            if (!sellerIds.isEmpty()) {
                sysUserMapper.selectBatchIds(sellerIds).forEach(s -> sellerMap.put(s.getId(), s));
            }
        }

        List<GoodsListVO> voList = goodsList.stream()
                .map(g -> convertToGoodsListVO(g, catMap, sellerMap))
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    /**
     * 将 GoodsInfo 转换为 GoodsListVO（单个查询版本，供收藏/浏览历史等调用）
     */
    private GoodsListVO convertToGoodsListVO(GoodsInfo goods) {
        return convertToGoodsListVO(goods, null, null);
    }

    /**
     * 将 GoodsInfo 转换为 GoodsListVO（支持批量 Map 缓存）
     */
    private GoodsListVO convertToGoodsListVO(GoodsInfo goods, Map<Long, GoodsCategory> catMap, Map<Long, SysUser> sellerMap) {
        GoodsListVO vo = new GoodsListVO();
        vo.setId(goods.getId());
        vo.setTitle(goods.getTitle());
        vo.setPrice(goods.getPrice());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setGoodsCondition(goods.getGoodsCondition());
        vo.setConditionDesc(getConditionDesc(goods.getGoodsCondition()));
        vo.setCoverImage(getFirstImage(goods.getImages()));
        vo.setCategoryId(goods.getCategoryId());
        vo.setStatus(goods.getStatus());
        vo.setStatusDesc(getStatusDesc(goods.getStatus()));
        vo.setTakedownBy(goods.getTakedownBy());
        vo.setTakedownReason(goods.getTakedownReason());
        vo.setViewCount(goods.getViewCount());
        vo.setFavoriteCount(goods.getFavoriteCount());
        vo.setCreateTime(goods.getCreateTime());
        vo.setTradeLocation(goods.getTradeLocation());

        // 查询分类名称（优先用批量缓存，否则单查）
        GoodsCategory category = catMap != null ? catMap.get(goods.getCategoryId())
                : goodsCategoryMapper.selectById(goods.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // 查询卖家信息（优先用批量缓存，否则单查）
        SysUser seller = sellerMap != null ? sellerMap.get(goods.getSellerId())
                : sysUserMapper.selectById(goods.getSellerId());
        if (seller != null) {
            vo.setSellerId(seller.getId());
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
        }

        return vo;
    }

    /**
     * 解析图片 JSON 为 List
     */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        return JSONUtil.toList(imagesJson, String.class);
    }

    /**
     * 获取第一张图片作为封面
     */
    private String getFirstImage(String imagesJson) {
        List<String> images = parseImages(imagesJson);
        return images.isEmpty() ? null : images.get(0);
    }

    /**
     * 获取成色描述
     */
    private String getConditionDesc(Integer condition) {
        if (condition == null || condition < 1 || condition >= CONDITION_DESCS.length) {
            return "未知";
        }
        return CONDITION_DESCS[condition];
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return STATUS_DESCS[status];
    }

    /**
     * 获取联系方式描述
     */
    private String getContactMethodDesc(Integer method) {
        switch (method) {
            case 1: return "手机";
            case 2: return "QQ";
            case 3: return "微信";
            default: return "未知";
        }
    }

    /**
     * 手机号脱敏：138****8888
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
