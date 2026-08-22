package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.CreateReportDTO;
import com.campus.market.service.ReportService;
import com.campus.market.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 举报控制器
 *
 * 接口列表：
 * - POST  /report       提交举报（需登录）
 * - GET   /report/my    我提交的举报列表（需登录）
 * - GET   /report/{id}  举报详情（需登录）
 *
 * 管理员处理举报接口在 Step 7 后台管理模块中实现
 */
@RestController
@RequestMapping("/report")
@Tag(name = "举报管理", description = "举报提交与查询")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    @Operation(summary = "提交举报", description = "举报用户或商品，不能举报自己，举报提交后不可修改/撤回")
    public Result<Long> createReport(@Valid @RequestBody CreateReportDTO dto) {
        Long id = reportService.createReport(dto);
        return Result.success(id);
    }

    @GetMapping("/my")
    @Operation(summary = "我提交的举报列表", description = "分页查询当前用户提交的举报记录")
    public Result<Page<ReportVO>> getMyReports(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ReportVO> page = reportService.getMyReports(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "举报详情", description = "查看举报详情，仅举报人本人可查看")
    public Result<ReportVO> getReportDetail(@PathVariable Long id) {
        ReportVO vo = reportService.getReportDetail(id);
        return Result.success(vo);
    }
}
