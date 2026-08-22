package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.*;
import com.campus.market.vo.*;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

/**
 * 后台管理服务接口
 *
 * 所有方法调用前必须通过 JwtInterceptor.requireAdmin() 校验管理员权限
 *
 * 功能模块：
 * 1. 仪表盘统计
 * 2. 用户管理（列表/详情/禁用/启用/重置密码/调整信用分）
 * 3. 商品管理（列表/详情/强制下架）
 * 4. 订单管理（列表/详情）
 * 5. 举报管理（列表/详情/处理举报）
 * 6. 分类管理（列表/新增/编辑/启用禁用）
 */
public interface AdminService {

    // ================== 仪表盘 ==================

    /**
     * 获取仪表盘统计数据
     */
    AdminDashboardVO getDashboard();

    // ================== 用户管理 ==================

    /**
     * 用户列表（分页+搜索+筛选）
     */
    Page<AdminUserVO> getUserList(AdminUserQueryDTO query);

    /**
     * 用户详情（含解密手机号）
     */
    AdminUserVO getUserDetail(Long userId);

    /**
     * 禁用用户
     */
    void banUser(Long userId);

    /**
     * 启用用户
     */
    void enableUser(Long userId);

    /**
     * 重置用户密码（重置为默认密码 admin123）
     */
    void resetUserPassword(Long userId);

    /**
     * 管理员调整用户信用分
     */
    void adjustCreditScore(Long userId, AdminAdjustCreditDTO dto);

    // ================== 商品管理 ==================

    /**
     * 商品列表（分页+搜索+筛选）
     */
    Page<AdminGoodsVO> getGoodsList(AdminGoodsQueryDTO query);

    /**
     * 商品详情（含卖家信息+解密联系方式）
     */
    AdminGoodsVO getGoodsDetail(Long goodsId);

    /**
     * 管理员强制下架商品（打强制下架标记，卖家需修改后提交审核才能重新上架）
     */
    void forceTakedownGoods(Long goodsId, String reason);

    /**
     * 审核强制下架商品的重新上架申请（通过：上架并清除标记；驳回：保持下架并通知卖家原因）
     */
    void reviewGoods(Long goodsId, AdminReviewGoodsDTO dto);

    // ================== 订单管理 ==================

    /**
     * 订单列表（分页+搜索+筛选）
     */
    Page<AdminOrderVO> getOrderList(AdminOrderQueryDTO query);

    /**
     * 订单详情（含买家/卖家信息）
     */
    AdminOrderVO getOrderDetail(Long orderId);

    // ================== 举报管理 ==================

    /**
     * 举报列表（分页+按状态筛选）
     */
    Page<ReportVO> getReportList(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 举报详情
     */
    ReportVO getReportDetail(Long reportId);

    /**
     * 处理举报（警告/下架商品/封禁账号/驳回）
     */
    void handleReport(Long reportId, AdminHandleReportDTO dto);

    // ================== 分类管理 ==================

    /**
     * 分类列表（含商品数量统计）
     */
    Page<AdminCategoryVO> getCategoryList(Integer pageNum, Integer pageSize);

    /**
     * 新增分类
     */
    Long createCategory(@Valid @RequestBody AdminCategoryDTO dto);

    /**
     * 编辑分类
     */
    void updateCategory(Long id, @Valid @RequestBody AdminCategoryDTO dto);

    /**
     * 启用/禁用分类
     */
    void toggleCategoryStatus(Long id, Integer status);
}
