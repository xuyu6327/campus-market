package com.campus.market.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.*;
import com.campus.market.entity.*;
import com.campus.market.mapper.*;
import com.campus.market.service.AdminService;
import com.campus.market.service.NotificationService;
import com.campus.market.service.SysUserService;
import com.campus.market.util.CryptoUtils;
import com.campus.market.util.PageUtil;
import com.campus.market.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台管理服务实现类
 *
 * 核心逻辑：
 * 1. 所有方法入口校验管理员权限（JwtInterceptor.requireAdmin()）
 * 2. 仪表盘：实时查询各表 COUNT 统计
 * 3. 用户管理：列表搜索/禁用启用/重置密码/调整信用分
 * 4. 商品管理：列表搜索/强制下架
 * 5. 订单管理：列表搜索/详情查看
 * 6. 举报管理：处理举报（警告/下架/封禁/驳回）+ 通知相关方
 * 7. 分类管理：CRUD + 启用禁用
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private GoodsInfoMapper goodsInfoMapper;
    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;
    @Autowired
    private TradeOrderMapper tradeOrderMapper;
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private CryptoUtils cryptoUtils;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private SysUserService sysUserService;

    /** 成色描述（与前端 CONDITION_MAP 一致；index 6 兼容旧数据） */
    private static final String[] CONDITION_DESCS = {
            null, "全新未拆", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "故障/坏件", "故障/坏件"
    };

    /** 商品状态描述 */
    private static final String[] GOODS_STATUS_DESCS = {
            "下架", "在售", "预订中", "已售出", "待审核"
    };

    /** 订单状态描述 */
    private static final String[] ORDER_STATUS_DESCS = {
            "待交易", "已完成", "买家取消", "卖家取消", "超时自动取消"
    };

    /** 举报状态描述 */
    private static final String[] REPORT_STATUS_DESCS = {
            "待处理", "警告", "下架商品", "封禁账号", "驳回"
    };

    /** 默认重置密码 */
    private static final String DEFAULT_PASSWORD = "admin123";

    /** 举报信用分处罚（被举报核实/恶意举报均扣此分值） */
    private static final int REPORT_CREDIT_PENALTY = -10;

    // ============================================================
    // 1. 仪表盘
    // ============================================================

    @Override
    public AdminDashboardVO getDashboard() {
        JwtInterceptor.requireAdmin();

        AdminDashboardVO vo = new AdminDashboardVO();

        // 累计统计
        vo.setTotalUsers(sysUserMapper.selectCount(null));
        vo.setTotalGoods(goodsInfoMapper.selectCount(null));
        vo.setTotalOrders(tradeOrderMapper.selectCount(null));
        vo.setTotalTrades(tradeOrderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getStatus, 1)
        ));

        // 今日统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        vo.setTodayNewUsers(sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .ge(SysUser::getCreateTime, todayStart)
                        .lt(SysUser::getCreateTime, todayEnd)
        ));
        vo.setTodayNewGoods(goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>()
                        .ge(GoodsInfo::getCreateTime, todayStart)
                        .lt(GoodsInfo::getCreateTime, todayEnd)
        ));
        vo.setTodayTradedGoods(tradeOrderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, 1)
                        .ge(TradeOrder::getTradeTime, todayStart)
                        .lt(TradeOrder::getTradeTime, todayEnd)
        ));
        vo.setTodayCancelledOrders(tradeOrderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>()
                        .in(TradeOrder::getStatus, 2, 3, 4)
                        .ge(TradeOrder::getUpdateTime, todayStart)
                        .lt(TradeOrder::getUpdateTime, todayEnd)
        ));

        // 待处理事项
        vo.setPendingReports(reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, 0)
        ));
        vo.setOnSaleGoods(goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>().eq(GoodsInfo::getStatus, 1)
        ));
        vo.setPendingOrders(tradeOrderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getStatus, 0)
        ));

        log.info("[仪表盘] 管理员={} 查看统计数据", JwtInterceptor.getCurrentUserId());
        return vo;
    }

    // ============================================================
    // 2. 用户管理
    // ============================================================

    @Override
    public Page<AdminUserVO> getUserList(AdminUserQueryDTO query) {
        JwtInterceptor.requireAdmin();

        Page<SysUser> page = PageUtil.of(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(query.getRole() != null, SysUser::getRole, query.getRole());

        // 关键词搜索：昵称 / 学号 / 真实姓名
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getNickname, kw)
                    .or().like(SysUser::getStudentId, kw)
                    .or().like(SysUser::getRealName, kw));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);
        return convertToAdminUserVOPage(userPage);
    }

    @Override
    public AdminUserVO getUserDetail(Long userId) {
        JwtInterceptor.requireAdmin();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return convertToAdminUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        if (userId.equals(adminId)) {
            throw new BizException(400, "不能禁用自己的账号");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (user.getRole() == 1) {
            throw new BizException(400, "不能禁用管理员账号");
        }
        if (user.getStatus() == 0) {
            throw new BizException(400, "该用户已被禁用");
        }

        sysUserMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getStatus, 0)
        );

        // 通知用户
        notificationService.sendNotification(
                userId, 7, "账号被禁用",
                "您的账号已被管理员禁用。如有疑问请联系管理员。",
                null
        );

        log.info("[禁用用户] admin={}, userId={}", adminId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (user.getStatus() == 1) {
            throw new BizException(400, "该用户已是正常状态");
        }
        if (user.getCreditScore() != null && user.getCreditScore() < 10) {
            throw new BizException(400, "该用户信用分低于10（处于冻结状态），请先调整信用分后再启用");
        }

        sysUserMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getStatus, 1)
        );

        // 通知用户
        notificationService.sendNotification(
                userId, 7, "账号已恢复",
                "您的账号已被管理员恢复使用。",
                null
        );

        log.info("[启用用户] admin={}, userId={}", adminId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        String hashedPassword = BCrypt.hashpw(DEFAULT_PASSWORD);
        sysUserMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getPassword, hashedPassword)
        );

        // 通知用户
        notificationService.sendNotification(
                userId, 7, "密码已重置",
                "您的密码已被管理员重置为默认密码，请尽快登录修改。",
                null
        );

        log.info("[重置密码] admin={}, userId={}", adminId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustCreditScore(Long userId, AdminAdjustCreditDTO dto) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (dto.getChangeValue() == 0) {
            throw new BizException(400, "变更分值不能为0");
        }

        // 调用 SysUserService 的信用分变更方法（含日志记录 + 乐观锁）
        sysUserService.updateCreditScore(userId, dto.getChangeValue(),
                "管理员调整: " + dto.getReason(), null, adminId);

        // 通知用户
        String direction = dto.getChangeValue() > 0 ? "增加" : "扣除";
        notificationService.sendNotification(
                userId, 7, "信用分调整",
                "管理员" + direction + "了您 " + Math.abs(dto.getChangeValue()) + " 信用分，原因: " + dto.getReason(),
                null
        );

        log.info("[调整信用分] admin={}, userId={}, change={}", adminId, userId, dto.getChangeValue());
    }

    // ============================================================
    // 3. 商品管理
    // ============================================================

    @Override
    public Page<AdminGoodsVO> getGoodsList(AdminGoodsQueryDTO query) {
        JwtInterceptor.requireAdmin();

        Page<GoodsInfo> page = PageUtil.of(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(query.getStatus() != null, GoodsInfo::getStatus, query.getStatus())
                .eq(query.getCategoryId() != null, GoodsInfo::getCategoryId, query.getCategoryId());

        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            wrapper.like(GoodsInfo::getTitle, query.getKeyword().trim());
        }

        wrapper.orderByDesc(GoodsInfo::getCreateTime);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        return convertToAdminGoodsVOPage(goodsPage);
    }

    @Override
    public AdminGoodsVO getGoodsDetail(Long goodsId) {
        JwtInterceptor.requireAdmin();

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        return convertToAdminGoodsVO(goods);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceTakedownGoods(Long goodsId, String reason) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (goods.getStatus() == 0) {
            throw new BizException(400, "该商品已被下架");
        }
        if (goods.getStatus() == 3) {
            throw new BizException(400, "已售出商品不可下架");
        }

        // 打强制下架标记：卖家无法直接重新上架，需修改后提交审核
        String reasonText = (reason == null || reason.trim().isEmpty()) ? "未填写" : reason.trim();
        goodsInfoMapper.update(null,
                new LambdaUpdateWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getId, goodsId)
                        .set(GoodsInfo::getStatus, 0)
                        .set(GoodsInfo::getTakedownBy, 1)
                        .set(GoodsInfo::getTakedownReason, reasonText)
        );

        // 通知卖家（说明原因 + 审核流程）
        notificationService.sendNotification(
                goods.getSellerId(), 7, "商品被强制下架",
                limitLen("您的商品「" + goods.getTitle() + "」已被管理员强制下架。原因: " + reasonText + "。请修改商品后提交重新上架申请，审核通过后可恢复在售。", 500),
                goodsId
        );

        log.info("[强制下架] admin={}, goodsId={}, title={}", adminId, goodsId, goods.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewGoods(Long goodsId, AdminReviewGoodsDTO dto) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        if (dto.getApprove() == null) {
            throw new BizException(400, "缺少审核结果");
        }

        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException(404, "商品不存在");
        }
        if (goods.getStatus() != 4) {
            throw new BizException(400, "该商品不在待审核状态");
        }

        if (Boolean.TRUE.equals(dto.getApprove())) {
            // 通过：上架 + 清除强制下架标记
            goodsInfoMapper.update(null,
                    new LambdaUpdateWrapper<GoodsInfo>()
                            .eq(GoodsInfo::getId, goodsId)
                            .set(GoodsInfo::getStatus, 1)
                            .set(GoodsInfo::getTakedownBy, 0)
                            .set(GoodsInfo::getTakedownReason, null)
                            .set(GoodsInfo::getLastRelistedAt, LocalDateTime.now())
            );
            notificationService.sendNotification(
                    goods.getSellerId(), 7, "商品审核通过",
                    "您的商品「" + goods.getTitle() + "」已通过审核并重新上架。",
                    goodsId
            );
            log.info("[审核通过] admin={}, goodsId={}", adminId, goodsId);
        } else {
            // 驳回：保持下架，记录驳回原因，通知卖家可再次修改提交
            String reasonText = (dto.getReason() == null || dto.getReason().trim().isEmpty()) ? "未填写" : dto.getReason().trim();
            goodsInfoMapper.update(null,
                    new LambdaUpdateWrapper<GoodsInfo>()
                            .eq(GoodsInfo::getId, goodsId)
                            .set(GoodsInfo::getStatus, 0)
                            .set(GoodsInfo::getTakedownBy, 1)
                            .set(GoodsInfo::getTakedownReason, reasonText)
            );
            notificationService.sendNotification(
                    goods.getSellerId(), 7, "商品审核未通过",
                    limitLen("您的商品「" + goods.getTitle() + "」未通过审核。原因: " + reasonText + "。请修改后重新提交审核。", 500),
                    goodsId
            );
            log.info("[审核驳回] admin={}, goodsId={}", adminId, goodsId);
        }
    }

    // ============================================================
    // 4. 订单管理
    // ============================================================

    @Override
    public Page<AdminOrderVO> getOrderList(AdminOrderQueryDTO query) {
        JwtInterceptor.requireAdmin();

        Page<TradeOrder> page = PageUtil.of(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .eq(query.getStatus() != null, TradeOrder::getStatus, query.getStatus());

        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(TradeOrder::getOrderNo, kw)
                    .or().like(TradeOrder::getGoodsTitle, kw));
        }

        wrapper.orderByDesc(TradeOrder::getCreateTime);

        Page<TradeOrder> orderPage = tradeOrderMapper.selectPage(page, wrapper);
        return convertToAdminOrderVOPage(orderPage);
    }

    @Override
    public AdminOrderVO getOrderDetail(Long orderId) {
        JwtInterceptor.requireAdmin();

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return convertToAdminOrderVO(order);
    }

    // ============================================================
    // 5. 举报管理
    // ============================================================

    @Override
    public Page<ReportVO> getReportList(Integer pageNum, Integer pageSize, Integer status) {
        JwtInterceptor.requireAdmin();

        Page<Report> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(status != null, Report::getStatus, status)
                .orderByDesc(Report::getCreateTime);

        Page<Report> reportPage = reportMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<ReportVO> result = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        List<ReportVO> voList = reportPage.getRecords().stream()
                .map(this::convertToReportVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    @Override
    public ReportVO getReportDetail(Long reportId) {
        JwtInterceptor.requireAdmin();

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(404, "举报记录不存在");
        }
        return convertToReportVO(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, AdminHandleReportDTO dto) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(404, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BizException(400, "该举报已被处理，不可重复处理");
        }
        if (dto.getStatus() == null || dto.getStatus() < 1 || dto.getStatus() > 4) {
            throw new BizException(400, "无效的处理状态（1警告/2下架/3封禁/4驳回）");
        }

        // 校验处理动作与举报对象的匹配性
        if (dto.getStatus() == 2 && report.getTargetType() != 2) {
            throw new BizException(400, "下架商品操作仅适用于举报商品类型的举报");
        }
        if (dto.getStatus() == 3 && report.getTargetType() != 1) {
            throw new BizException(400, "封禁账号操作仅适用于举报用户类型的举报");
        }

        // 更新举报记录
        reportMapper.update(null,
                new LambdaUpdateWrapper<Report>()
                        .eq(Report::getId, reportId)
                        .set(Report::getStatus, dto.getStatus())
                        .set(Report::getHandlerId, adminId)
                        .set(Report::getHandleResult, dto.getHandleResult())
                        .set(Report::getHandleTime, LocalDateTime.now())
        );

        String statusDesc = REPORT_STATUS_DESCS[dto.getStatus()];

        // 举报属实（警告/下架/封禁）时，扣被举报人信用分
        if (dto.getStatus() != 4) {
            Long targetUserId = null;
            if (report.getTargetType() == 1) {
                targetUserId = report.getTargetId();
            } else {
                GoodsInfo g = goodsInfoMapper.selectById(report.getTargetId());
                if (g != null) {
                    targetUserId = g.getSellerId();
                }
            }
            if (targetUserId != null) {
                try {
                    sysUserService.updateCreditScore(targetUserId, REPORT_CREDIT_PENALTY, "举报核实违规", reportId, adminId);
                } catch (Exception e) {
                    log.error("[举报信用分联动失败] targetUserId={}, reportId={}", targetUserId, reportId, e);
                }
            }
        }

        // 根据处理动作执行副作用（通知内容均带上商品名与举报原因，使被举报方明确知道是哪方面出了问题）
        String reasonText = report.getReason() == null ? "未填写" : report.getReason();
        switch (dto.getStatus()) {
            case 1: // 警告
                // 警告对象：举报用户 → 该用户；举报商品 → 商品卖家（避免 sendNotification(null) 导致 user_id NOT NULL 插入失败）
                Long warnUserId = null;
                String warnContent = "您因举报被管理员警告: " + dto.getHandleResult() + "（举报原因: " + reasonText + "）";
                if (report.getTargetType() == 1) {
                    warnUserId = report.getTargetId();
                } else {
                    GoodsInfo warnGoods = goodsInfoMapper.selectById(report.getTargetId());
                    if (warnGoods != null) {
                        warnUserId = warnGoods.getSellerId();
                        warnContent = "您的商品「" + warnGoods.getTitle() + "」因举报被管理员警告: " + dto.getHandleResult() + "（举报原因: " + reasonText + "）";
                    }
                }
                if (warnUserId != null) {
                    notificationService.sendNotification(
                            warnUserId, 7, "警告通知",
                            limitLen(warnContent, 500),
                            null
                    );
                }
                break;

            case 2: // 下架商品（打强制下架标记，卖家需修改后提交审核才能重新上架）
                GoodsInfo goods = goodsInfoMapper.selectById(report.getTargetId());
                if (goods != null && goods.getStatus() != 0) {
                    goodsInfoMapper.update(null,
                            new LambdaUpdateWrapper<GoodsInfo>()
                                    .eq(GoodsInfo::getId, report.getTargetId())
                                    .set(GoodsInfo::getStatus, 0)
                                    .set(GoodsInfo::getTakedownBy, 1)
                                    .set(GoodsInfo::getTakedownReason, limitLen("举报核实违规: " + report.getReason(), 200))
                    );
                }
                if (goods != null) {
                    String takedownContent = "您的商品「" + goods.getTitle() + "」因举报被管理员下架: " + dto.getHandleResult() + "（举报原因: " + reasonText + "）";
                    notificationService.sendNotification(
                            goods.getSellerId(), 7, "商品被下架",
                            limitLen(takedownContent, 500),
                            goods.getId()
                    );
                }
                break;

            case 3: // 封禁账号
                SysUser banTarget = sysUserMapper.selectById(report.getTargetId());
                if (banTarget != null && banTarget.getRole() == 1) {
                    throw new BizException(400, "不能封禁管理员账号");
                }
                sysUserMapper.update(null,
                        new LambdaUpdateWrapper<SysUser>()
                                .eq(SysUser::getId, report.getTargetId())
                                .set(SysUser::getStatus, 0)
                );
                String banContent = "您的账号因举报被管理员封禁: " + dto.getHandleResult() + "（举报原因: " + reasonText + "）";
                notificationService.sendNotification(
                        report.getTargetId(), 7, "账号被封禁",
                        limitLen(banContent, 500),
                        null
                );
                break;

            case 4: // 驳回
                // 若标记为恶意举报，扣举报人信用分
                if (Boolean.TRUE.equals(dto.getIsMalicious())) {
                    try {
                        sysUserService.updateCreditScore(report.getReporterId(), REPORT_CREDIT_PENALTY, "恶意举报", reportId, adminId);
                    } catch (Exception e) {
                        log.error("[恶意举报信用分联动失败] reporterId={}, reportId={}", report.getReporterId(), reportId, e);
                    }
                }
                break;

            default:
                throw new BizException(400, "无效的处理状态");
        }

        // 通知举报人处理结果（带被举报对象名称）
        String targetTypeDesc;
        if (report.getTargetType() == 2) {
            GoodsInfo reporterGoods = goodsInfoMapper.selectById(report.getTargetId());
            targetTypeDesc = reporterGoods != null ? "商品「" + reporterGoods.getTitle() + "」" : "商品";
        } else {
            targetTypeDesc = "用户";
        }
        String reporterContent = "您举报的" + targetTypeDesc + "已处理，处理结果: " + statusDesc + "。说明: " + dto.getHandleResult() + "（举报原因: " + reasonText + "）";
        // relatedId 语义：前端通知点击跳转规则是 type=7 → 商品详情页。
        // 商品举报传商品 id 可正确跳转；用户举报无商品可跳，传 null 不跳转（避免误跳错误页面）
        Long reporterRelatedId = (report.getTargetType() == 2) ? report.getTargetId() : null;
        notificationService.sendNotification(
                report.getReporterId(), 7, "举报处理结果",
                limitLen(reporterContent, 500),
                reporterRelatedId
        );

        log.info("[处理举报] admin={}, reportId={}, status={}", adminId, reportId, dto.getStatus());
    }

    /** 截断字符串（通知 content 字段上限 500 字符，超长会插入失败） */
    private String limitLen(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ============================================================
    // 6. 分类管理
    // ============================================================

    @Override
    public Page<AdminCategoryVO> getCategoryList(Integer pageNum, Integer pageSize) {
        JwtInterceptor.requireAdmin();

        Page<GoodsCategory> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<GoodsCategory> wrapper = new LambdaQueryWrapper<GoodsCategory>()
                .orderByAsc(GoodsCategory::getSortOrder);

        Page<GoodsCategory> categoryPage = goodsCategoryMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<AdminCategoryVO> result = new Page<>(categoryPage.getCurrent(), categoryPage.getSize(), categoryPage.getTotal());
        List<AdminCategoryVO> voList = categoryPage.getRecords().stream()
                .map(this::convertToAdminCategoryVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(AdminCategoryDTO dto) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        // 校验父分类
        if (dto.getParentId() != 0) {
            GoodsCategory parent = goodsCategoryMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BizException(400, "父分类不存在");
            }
            if (parent.getParentId() != 0) {
                throw new BizException(400, "仅支持两级分类");
            }
        }

        // 检查同级分类名称是否重复
        Long count = goodsCategoryMapper.selectCount(
                new LambdaQueryWrapper<GoodsCategory>()
                        .eq(GoodsCategory::getParentId, dto.getParentId())
                        .eq(GoodsCategory::getName, dto.getName())
        );
        if (count > 0) {
            throw new BizException(400, "同级下已存在同名分类");
        }

        GoodsCategory category = new GoodsCategory();
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        goodsCategoryMapper.insert(category);
        log.info("[新增分类] admin={}, categoryId={}, name={}", adminId, category.getId(), category.getName());
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, AdminCategoryDTO dto) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        GoodsCategory category = goodsCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }

        // 校验父分类
        if (dto.getParentId() != 0) {
            if (dto.getParentId().equals(id)) {
                throw new BizException(400, "不能将自身设为父分类");
            }
            GoodsCategory parent = goodsCategoryMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BizException(400, "父分类不存在");
            }
            if (parent.getParentId() != 0) {
                throw new BizException(400, "仅支持两级分类");
            }
        }

        // 检查同级分类名称是否重复（排除自身）
        Long count = goodsCategoryMapper.selectCount(
                new LambdaQueryWrapper<GoodsCategory>()
                        .eq(GoodsCategory::getParentId, dto.getParentId())
                        .eq(GoodsCategory::getName, dto.getName())
                        .ne(GoodsCategory::getId, id)
        );
        if (count > 0) {
            throw new BizException(400, "同级下已存在同名分类");
        }

        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }

        goodsCategoryMapper.updateById(category);
        log.info("[编辑分类] admin={}, categoryId={}, name={}", adminId, id, dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCategoryStatus(Long id, Integer status) {
        JwtInterceptor.requireAdmin();
        Long adminId = JwtInterceptor.getCurrentUserId();

        GoodsCategory category = goodsCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }

        if (status != 0 && status != 1) {
            throw new BizException(400, "无效的状态值");
        }

        goodsCategoryMapper.update(null,
                new LambdaUpdateWrapper<GoodsCategory>()
                        .eq(GoodsCategory::getId, id)
                        .set(GoodsCategory::getStatus, status)
        );

        log.info("[切换分类状态] admin={}, categoryId={}, status={}", adminId, id, status);
    }

    // ============================================================
    // 工具方法 — VO 转换
    // ============================================================

    // ---------- 用户 VO ----------

    private Page<AdminUserVO> convertToAdminUserVOPage(Page<SysUser> userPage) {
        Page<AdminUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<AdminUserVO> voList = userPage.getRecords().stream()
                .map(this::convertToAdminUserVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    private AdminUserVO convertToAdminUserVO(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStudentId(user.getStudentId());
        vo.setRealName(user.getRealName());
        vo.setQq(user.getQq());
        vo.setWechat(user.getWechat());
        vo.setCreditScore(user.getCreditScore());
        vo.setRole(user.getRole());
        vo.setRoleDesc(user.getRole() == 1 ? "管理员" : "普通用户");
        vo.setStatus(user.getStatus());
        vo.setStatusDesc(user.getStatus() == 1 ? "正常" : "禁用");
        vo.setCancelStatus(user.getCancelStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());

        // 管理员可见解密手机号
        if (user.getPhone() != null) {
            try {
                vo.setPhone(cryptoUtils.decryptPhone(user.getPhone()));
            } catch (Exception e) {
                vo.setPhone("[解密失败]");
            }
        }

        return vo;
    }

    // ---------- 商品 VO ----------

    private Page<AdminGoodsVO> convertToAdminGoodsVOPage(Page<GoodsInfo> goodsPage) {
        Page<AdminGoodsVO> result = new Page<>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal());
        List<AdminGoodsVO> voList = goodsPage.getRecords().stream()
                .map(this::convertToAdminGoodsVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    private AdminGoodsVO convertToAdminGoodsVO(GoodsInfo goods) {
        AdminGoodsVO vo = new AdminGoodsVO();
        vo.setId(goods.getId());
        vo.setSellerId(goods.getSellerId());
        vo.setTitle(goods.getTitle());
        vo.setDescription(goods.getDescription());
        vo.setCategoryId(goods.getCategoryId());
        vo.setPrice(goods.getPrice());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setGoodsCondition(goods.getGoodsCondition());
        vo.setConditionDesc(getConditionDesc(goods.getGoodsCondition()));
        vo.setImages(goods.getImages());
        vo.setTradeLocation(goods.getTradeLocation());
        vo.setContactMethod(goods.getContactMethod());
        vo.setContactQq(goods.getContactQq());
        vo.setContactWechat(goods.getContactWechat());
        vo.setStatus(goods.getStatus());
        vo.setStatusDesc(getGoodsStatusDesc(goods.getStatus()));
        vo.setTakedownBy(goods.getTakedownBy());
        vo.setTakedownReason(goods.getTakedownReason());
        vo.setViewCount(goods.getViewCount());
        vo.setFavoriteCount(goods.getFavoriteCount());
        vo.setLastRelistedAt(goods.getLastRelistedAt());
        vo.setCreateTime(goods.getCreateTime());
        vo.setUpdateTime(goods.getUpdateTime());

        // 管理员可见解密联系手机号
        if (goods.getContactPhone() != null) {
            try {
                vo.setContactPhone(cryptoUtils.decryptPhone(goods.getContactPhone()));
            } catch (Exception e) {
                vo.setContactPhone("[解密失败]");
            }
        }

        // 查询卖家昵称
        SysUser seller = sysUserMapper.selectById(goods.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
        }

        // 查询分类名称
        GoodsCategory category = goodsCategoryMapper.selectById(goods.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        return vo;
    }

    // ---------- 订单 VO ----------

    private Page<AdminOrderVO> convertToAdminOrderVOPage(Page<TradeOrder> orderPage) {
        Page<AdminOrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<AdminOrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToAdminOrderVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    private AdminOrderVO convertToAdminOrderVO(TradeOrder order) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setGoodsId(order.getGoodsId());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setSellerQq(order.getSellerQq());
        vo.setSellerWechat(order.getSellerWechat());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getOrderStatusDesc(order.getStatus()));
        vo.setContactFailAt(order.getContactFailAt());
        vo.setTradeTime(order.getTradeTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        // 管理员可见解密买家手机号
        if (order.getBuyerPhone() != null) {
            try {
                vo.setBuyerPhone(cryptoUtils.decryptPhone(order.getBuyerPhone()));
            } catch (Exception e) {
                vo.setBuyerPhone("[解密失败]");
            }
        }

        // 查询商品信息
        GoodsInfo goods = goodsInfoMapper.selectById(order.getGoodsId());
        if (goods != null) {
            vo.setGoodsTitle(goods.getTitle());
            vo.setGoodsPrice(goods.getPrice());
            // 取第一张图片
            if (goods.getImages() != null && !goods.getImages().isEmpty()) {
                String images = goods.getImages();
                // 简单取第一张：去掉 [ 和 " 后取到下一个 " 为止
                String img = images.replace("[", "").replace("]", "").trim();
                if (img.startsWith("\"")) {
                    img = img.substring(1);
                }
                int idx = img.indexOf("\"");
                if (idx > 0) {
                    img = img.substring(0, idx);
                }
                vo.setGoodsImage(img);
            }
        }

        // 查询买家昵称
        SysUser buyer = sysUserMapper.selectById(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerNickname(buyer.getNickname());
        }

        // 查询卖家昵称
        SysUser seller = sysUserMapper.selectById(order.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
        }

        return vo;
    }

    // ---------- 举报 VO ----------

    private ReportVO convertToReportVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setTargetType(report.getTargetType());
        vo.setTargetTypeDesc(report.getTargetType() == 1 ? "用户" : "商品");
        vo.setTargetId(report.getTargetId());
        vo.setReason(report.getReason());
        vo.setDescription(report.getDescription());
        vo.setStatus(report.getStatus());
        vo.setStatusDesc(getReportStatusDesc(report.getStatus()));
        vo.setHandlerId(report.getHandlerId());
        vo.setHandleResult(report.getHandleResult());
        vo.setCreateTime(report.getCreateTime());
        vo.setHandleTime(report.getHandleTime());

        // 查询举报人昵称
        SysUser reporter = sysUserMapper.selectById(report.getReporterId());
        if (reporter != null) {
            vo.setReporterNickname(reporter.getNickname());
        }

        // 查询举报对象名称
        if (report.getTargetType() == 1) {
            SysUser targetUser = sysUserMapper.selectById(report.getTargetId());
            if (targetUser != null) {
                vo.setTargetName(targetUser.getNickname());
            }
        } else if (report.getTargetType() == 2) {
            GoodsInfo targetGoods = goodsInfoMapper.selectById(report.getTargetId());
            if (targetGoods != null) {
                vo.setTargetName(targetGoods.getTitle());
            }
        }

        // 管理员视角下 isReporter 设为 false
        vo.setIsReporter(false);

        return vo;
    }

    // ---------- 分类 VO ----------

    private AdminCategoryVO convertToAdminCategoryVO(GoodsCategory category) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setSortOrder(category.getSortOrder());
        vo.setStatus(category.getStatus());
        vo.setStatusDesc(category.getStatus() == 1 ? "启用" : "禁用");
        vo.setCreateTime(category.getCreateTime());

        // 查询父分类名称
        if (category.getParentId() != 0) {
            GoodsCategory parent = goodsCategoryMapper.selectById(category.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getName());
            }
        }

        // 查询该分类下商品数量
        Long goodsCount = goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getCategoryId, category.getId())
        );
        vo.setGoodsCount(goodsCount.intValue());

        return vo;
    }

    // ---------- 描述工具方法 ----------

    private String getConditionDesc(Integer condition) {
        if (condition == null || condition < 1 || condition >= CONDITION_DESCS.length) {
            return "未知";
        }
        return CONDITION_DESCS[condition];
    }

    private String getGoodsStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return GOODS_STATUS_DESCS[status];
    }

    private String getOrderStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return ORDER_STATUS_DESCS[status];
    }

    private String getReportStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return REPORT_STATUS_DESCS[status];
    }
}
