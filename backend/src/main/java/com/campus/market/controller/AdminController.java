package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.*;
import com.campus.market.service.AdminService;
import com.campus.market.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理控制器
 *
 * 所有接口路径前缀: /admin
 * 所有接口均需登录 + 管理员权限（JwtInterceptor 拦截 + AdminService 内部 requireAdmin 校验）
 *
 * 接口分组：
 * 1. 仪表盘    GET /admin/dashboard
 * 2. 用户管理  GET/PUT /admin/user/**
 * 3. 商品管理  GET/PUT /admin/goods/**
 * 4. 订单管理  GET /admin/order/**
 * 5. 举报管理  GET/PUT /admin/report/**
 * 6. 分类管理  GET/POST/PUT /admin/category/**
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@Tag(name = "后台管理模块", description = "仪表盘、用户管理、商品管理、订单管理、举报管理、分类管理")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ================== 1. 仪表盘 ==================

    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘统计", description = "获取累计统计、今日统计、待处理事项等仪表盘数据")
    public Result<AdminDashboardVO> getDashboard() {
        AdminDashboardVO vo = adminService.getDashboard();
        return Result.success(vo);
    }

    // ================== 2. 用户管理 ==================

    @GetMapping("/user/list")
    @Operation(summary = "用户列表", description = "分页查询用户列表，支持按昵称/学号/真实姓名搜索，按状态和角色筛选")
    public Result<Page<AdminUserVO>> getUserList(AdminUserQueryDTO query) {
        Page<AdminUserVO> page = adminService.getUserList(query);
        return Result.success(page);
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "用户详情", description = "查看用户详情（管理员可见解密手机号）")
    public Result<AdminUserVO> getUserDetail(@PathVariable Long id) {
        AdminUserVO vo = adminService.getUserDetail(id);
        return Result.success(vo);
    }

    @PutMapping("/user/{id}/ban")
    @Operation(summary = "禁用用户", description = "禁用指定用户账号，不能禁用管理员和自己")
    public Result<Void> banUser(@PathVariable Long id) {
        adminService.banUser(id);
        return Result.success("禁用成功", null);
    }

    @PutMapping("/user/{id}/enable")
    @Operation(summary = "启用用户", description = "恢复被禁用的用户账号")
    public Result<Void> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return Result.success("启用成功", null);
    }

    @PutMapping("/user/{id}/reset-password")
    @Operation(summary = "重置密码", description = "将用户密码重置为默认密码 admin123")
    public Result<Void> resetUserPassword(@PathVariable Long id) {
        adminService.resetUserPassword(id);
        return Result.success("密码已重置为默认密码", null);
    }

    @PutMapping("/user/{id}/credit")
    @Operation(summary = "调整信用分", description = "管理员手动调整用户信用分，正数加分负数扣分，自动记录信用日志")
    public Result<Void> adjustCreditScore(@PathVariable Long id, @Validated @RequestBody AdminAdjustCreditDTO dto) {
        adminService.adjustCreditScore(id, dto);
        return Result.success("信用分调整成功", null);
    }

    // ================== 3. 商品管理 ==================

    @GetMapping("/goods/list")
    @Operation(summary = "商品列表", description = "分页查询商品列表，支持按标题搜索，按状态和分类筛选")
    public Result<Page<AdminGoodsVO>> getGoodsList(AdminGoodsQueryDTO query) {
        Page<AdminGoodsVO> page = adminService.getGoodsList(query);
        return Result.success(page);
    }

    @GetMapping("/goods/{id}")
    @Operation(summary = "商品详情", description = "查看商品详情（管理员可见解密联系方式+卖家信息）")
    public Result<AdminGoodsVO> getGoodsDetail(@PathVariable Long id) {
        AdminGoodsVO vo = adminService.getGoodsDetail(id);
        return Result.success(vo);
    }

    @PutMapping("/goods/{id}/takedown")
    @Operation(summary = "强制下架", description = "管理员强制下架商品（打强制下架标记，卖家需修改后提交审核才能重新上架），已售出商品不可下架")
    public Result<Void> forceTakedownGoods(@PathVariable Long id, @RequestBody(required = false) AdminTakedownGoodsDTO dto) {
        adminService.forceTakedownGoods(id, dto != null ? dto.getReason() : null);
        return Result.success("下架成功", null);
    }

    @PutMapping("/goods/{id}/review")
    @Operation(summary = "审核重新上架", description = "审核强制下架商品的重新上架申请：approve=true 通过上架；approve=false 驳回（reason 必填，通知卖家）")
    public Result<Void> reviewGoods(@PathVariable Long id, @RequestBody AdminReviewGoodsDTO dto) {
        adminService.reviewGoods(id, dto);
        return Result.success("审核完成", null);
    }

    // ================== 4. 订单管理 ==================

    @GetMapping("/order/list")
    @Operation(summary = "订单列表", description = "分页查询订单列表，支持按订单编号/商品标题搜索，按状态筛选")
    public Result<Page<AdminOrderVO>> getOrderList(AdminOrderQueryDTO query) {
        Page<AdminOrderVO> page = adminService.getOrderList(query);
        return Result.success(page);
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "订单详情", description = "查看订单详情（管理员可见买家/卖家完整信息）")
    public Result<AdminOrderVO> getOrderDetail(@PathVariable Long id) {
        AdminOrderVO vo = adminService.getOrderDetail(id);
        return Result.success(vo);
    }

    // ================== 5. 举报管理 ==================

    @GetMapping("/report/list")
    @Operation(summary = "举报列表", description = "分页查询举报列表，可按处理状态筛选")
    public Result<Page<ReportVO>> getReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<ReportVO> page = adminService.getReportList(pageNum, pageSize, status);
        return Result.success(page);
    }

    @GetMapping("/report/{id}")
    @Operation(summary = "举报详情", description = "查看举报详情")
    public Result<ReportVO> getReportDetail(@PathVariable Long id) {
        ReportVO vo = adminService.getReportDetail(id);
        return Result.success(vo);
    }

    @PutMapping("/report/{id}/handle")
    @Operation(summary = "处理举报", description = "管理员处理举报：1警告 2下架商品 3封禁账号 4驳回，处理后通知举报人和被举报人")
    public Result<Void> handleReport(@PathVariable Long id, @Validated @RequestBody AdminHandleReportDTO dto) {
        adminService.handleReport(id, dto);
        return Result.success("处理成功", null);
    }

    // ================== 6. 分类管理 ==================

    @GetMapping("/category/list")
    @Operation(summary = "分类列表", description = "分页查询分类列表，含商品数量统计")
    public Result<Page<AdminCategoryVO>> getCategoryList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<AdminCategoryVO> page = adminService.getCategoryList(pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/category")
    @Operation(summary = "新增分类", description = "新增商品分类，仅支持两级分类结构")
    public Result<Long> createCategory(@Validated @RequestBody AdminCategoryDTO dto) {
        Long id = adminService.createCategory(dto);
        return Result.success("新增成功", id);
    }

    @PutMapping("/category/{id}")
    @Operation(summary = "编辑分类", description = "编辑商品分类信息")
    public Result<Void> updateCategory(@PathVariable Long id, @Validated @RequestBody AdminCategoryDTO dto) {
        adminService.updateCategory(id, dto);
        return Result.success("编辑成功", null);
    }

    @PutMapping("/category/{id}/status")
    @Operation(summary = "启用/禁用分类", description = "切换分类状态：0禁用 1启用")
    public Result<Void> toggleCategoryStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.toggleCategoryStatus(id, status);
        return Result.success("状态切换成功", null);
    }
}
