package com.campus.market.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.RegisterDTO;
import com.campus.market.dto.UpdateUserDTO;
import com.campus.market.entity.CreditLog;
import com.campus.market.vo.LoginVO;
import com.campus.market.vo.UserInfoVO;
import com.campus.market.vo.UserPublicVO;

/**
 * 用户服务接口
 */
public interface SysUserService {

    /**
     * 用户注册
     * @param dto 注册信息（手机号、密码、昵称）
     * @return 新用户ID
     */
    Long register(RegisterDTO dto);

    /**
     * 用户登录
     * @param dto 登录信息（账号、密码）
     * @return 登录响应（含 JWT token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息
     * @return 用户信息（手机号脱敏）
     */
    UserInfoVO getUserInfo();

    /**
     * 修改当前登录用户信息
     * @param dto 修改信息
     */
    void updateUserInfo(UpdateUserDTO dto);

    /**
     * 获取当前用户信用分
     * @return 信用分
     */
    Integer getCreditScore();

    /**
     * 获取当前用户信用分变更记录
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页记录
     */
    Page<CreditLog> getCreditLogs(Integer pageNum, Integer pageSize);

    /**
     * 查看他人公开主页信息
     * @param userId 目标用户ID
     * @return 公开信息（昵称、头像、信用分、好评率、在售数等）
     */
    UserPublicVO getPublicProfile(Long userId);

    /**
     * 修改用户信用分（供其他模块调用）
     * @param userId      用户ID
     * @param changeValue 变更分值（正数加分，负数扣分）
     * @param reason      变更原因
     * @param orderId     关联订单ID（可为null）
     * @param operatorId  操作人ID（0=系统自动）
     */
    void updateCreditScore(Long userId, int changeValue, String reason, Long orderId, Long operatorId);
}
