package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.dto.CreateReportDTO;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.Report;
import com.campus.market.entity.SysUser;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.ReportMapper;
import com.campus.market.mapper.SysUserMapper;
import com.campus.market.service.NotificationService;
import com.campus.market.service.ReportService;
import com.campus.market.service.SensitiveWordService;
import com.campus.market.vo.ReportVO;
import com.campus.market.util.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报服务实现类
 *
 * 核心逻辑：
 * 1. 提交举报：校验举报对象存在 + 不能举报自己 -> 插入举报记录
 * 2. 我的举报列表：查看自己提交的举报
 * 3. 举报详情：仅举报人本人可查看
 * 4. 管理员处理举报在 Step 7 后台管理中实现
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    /** 举报对象类型描述 */
    private static final String[] TARGET_TYPE_DESCS = {null, "用户", "商品"};

    /** 举报状态描述 */
    private static final String[] STATUS_DESCS = {"待处理", "警告", "下架商品", "封禁账号", "驳回"};

    // ================== 提交举报 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(CreateReportDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[提交举报] reporterId={}, targetType={}, targetId={}",
                userId, dto.getTargetType(), dto.getTargetId());

        // 1. 不能举报自己
        if (dto.getTargetType() == 1 && dto.getTargetId().equals(userId)) {
            throw new BizException(400, "不能举报自己");
        }

        // 2. 校验举报对象存在
        String targetName = null;
        if (dto.getTargetType() == 1) {
            // 举报用户
            SysUser targetUser = sysUserMapper.selectById(dto.getTargetId());
            if (targetUser == null) {
                throw new BizException(404, "被举报用户不存在");
            }
            targetName = targetUser.getNickname();
        } else if (dto.getTargetType() == 2) {
            // 举报商品
            GoodsInfo targetGoods = goodsInfoMapper.selectById(dto.getTargetId());
            if (targetGoods == null) {
                throw new BizException(404, "被举报商品不存在");
            }
            if (targetGoods.getSellerId().equals(userId)) {
                throw new BizException(400, "不能举报自己的商品");
            }
            targetName = targetGoods.getTitle();
        }

        // 2.5 敏感词过滤（举报描述）
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()
                && sensitiveWordService.containsSensitive(dto.getDescription())) {
            throw new BizException(400, "举报描述包含敏感词，请修改后重新提交");
        }

        // 3. 构建举报记录
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription());
        report.setStatus(0); // 待处理

        reportMapper.insert(report);
        log.info("[举报创建成功] reportId={}, targetName={}", report.getId(), targetName);

        // 4. 发送系统通知给管理员（查询所有管理员）
        List<SysUser> admins = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, 1)
                        .eq(SysUser::getStatus, 1)
        );
        String typeDesc = dto.getTargetType() == 1 ? "用户" : "商品";
        for (SysUser admin : admins) {
            // relatedId 传 null：type=7 通知前端约定跳商品详情（relatedId=商品id），
            // 举报 id 会被误当商品 id 跳转错误页面；管理员处理举报入口在管理端，无需跳转
            notificationService.sendNotification(
                    admin.getId(),
                    7, // 系统通知
                    "新举报待处理",
                    "有用户举报了" + typeDesc + ": " + targetName + "，理由: " + dto.getReason(),
                    null
            );
        }

        return report.getId();
    }

    // ================== 我提交的举报列表 ==================

    @Override
    public Page<ReportVO> getMyReports(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<Report> page = PageUtil.of(pageNum, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .orderByDesc(Report::getCreateTime);

        Page<Report> reportPage = reportMapper.selectPage(page, wrapper);
        return convertToReportVOPage(reportPage, userId);
    }

    // ================== 举报详情 ==================

    @Override
    public ReportVO getReportDetail(Long reportId) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(404, "举报记录不存在");
        }

        // 权限校验：仅举报人本人可查看
        if (!report.getReporterId().equals(userId)) {
            throw new BizException(403, "无权查看此举报记录");
        }

        return convertToReportVO(report, userId);
    }

    // ================== 工具方法 ==================

    private Page<ReportVO> convertToReportVOPage(Page<Report> reportPage, Long currentUserId) {
        Page<ReportVO> result = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        List<ReportVO> voList = reportPage.getRecords().stream()
                .map(report -> convertToReportVO(report, currentUserId))
                .collect(Collectors.toList());
        result.setRecords(voList);
        return result;
    }

    private ReportVO convertToReportVO(Report report, Long currentUserId) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setTargetType(report.getTargetType());
        vo.setTargetTypeDesc(getTargetTypeDesc(report.getTargetType()));
        vo.setTargetId(report.getTargetId());
        vo.setReason(report.getReason());
        vo.setDescription(report.getDescription());
        vo.setStatus(report.getStatus());
        vo.setStatusDesc(getStatusDesc(report.getStatus()));
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
            // 用户
            SysUser targetUser = sysUserMapper.selectById(report.getTargetId());
            if (targetUser != null) {
                vo.setTargetName(targetUser.getNickname());
            }
        } else if (report.getTargetType() == 2) {
            // 商品
            GoodsInfo targetGoods = goodsInfoMapper.selectById(report.getTargetId());
            if (targetGoods != null) {
                vo.setTargetName(targetGoods.getTitle());
            }
        }

        // 当前用户身份标记
        vo.setIsReporter(report.getReporterId().equals(currentUserId));

        return vo;
    }

    private String getTargetTypeDesc(Integer targetType) {
        if (targetType == null || targetType < 1 || targetType > 2) {
            return "未知";
        }
        return TARGET_TYPE_DESCS[targetType];
    }

    private String getStatusDesc(Integer status) {
        if (status == null || status < 0 || status > 4) {
            return "未知";
        }
        return STATUS_DESCS[status];
    }
}
