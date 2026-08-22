package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.CreateReportDTO;
import com.campus.market.vo.ReportVO;

/**
 * 举报服务接口
 *
 * 举报对象类型：1=用户 2=商品
 * 处理状态：0待处理 1警告 2下架商品 3封禁账号 4驳回
 */
public interface ReportService {

    /**
     * 提交举报
     * 校验：举报对象存在、不能举报自己
     */
    Long createReport(CreateReportDTO dto);

    /**
     * 我提交的举报列表（分页）
     */
    Page<ReportVO> getMyReports(Integer pageNum, Integer pageSize);

    /**
     * 举报详情（仅举报人本人可查看）
     */
    ReportVO getReportDetail(Long reportId);
}
