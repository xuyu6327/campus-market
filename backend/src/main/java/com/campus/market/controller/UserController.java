package com.campus.market.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.Result;
import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.RegisterDTO;
import com.campus.market.dto.UpdateUserDTO;
import com.campus.market.entity.CreditLog;
import com.campus.market.service.SysUserService;
import com.campus.market.vo.LoginVO;
import com.campus.market.vo.UserInfoVO;
import com.campus.market.vo.UserPublicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * 接口列表：
 * - POST /user/register   注册（无需认证）
 * - POST /user/login      登录（无需认证）
 * - GET  /user/info       获取个人信息（需认证）
 * - PUT  /user/info       修改个人信息（需认证）
 * - GET  /user/credit     获取信用分（需认证）
 * - GET  /user/credit/logs 信用分变更记录（需认证）
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户模块", description = "注册、登录、个人信息、信用分")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    // ================== 注册 ==================

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "手机号+密码+昵称注册，手机号加密存储")
    public Result<Long> register(@Validated @RequestBody RegisterDTO dto) {
        log.info("[API] 注册请求: phone={}, nickname={}", dto.getPhone(), dto.getNickname());
        Long userId = sysUserService.register(dto);
        return Result.success("注册成功", userId);
    }

    // ================== 登录 ==================

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "手机号或学号+密码登录，返回JWT token")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        log.info("[API] 登录请求: account={}", dto.getAccount());
        LoginVO vo = sysUserService.login(dto);
        return Result.success("登录成功", vo);
    }

    // ================== 个人信息 ==================

    @GetMapping("/info")
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息（手机号脱敏）")
    public Result<UserInfoVO> getUserInfo() {
        UserInfoVO vo = sysUserService.getUserInfo();
        return Result.success(vo);
    }

    @PutMapping("/info")
    @Operation(summary = "修改个人信息", description = "修改昵称、头像、QQ、微信、学号、真实姓名")
    public Result<Void> updateUserInfo(@RequestBody UpdateUserDTO dto) {
        log.info("[API] 修改个人信息");
        sysUserService.updateUserInfo(dto);
        return Result.success("修改成功", null);
    }

    // ================== 查看他人公开主页 ==================

    @GetMapping("/{id}/profile")
    @Operation(summary = "查看他人公开主页", description = "查看指定用户的公开信息（昵称、信用分、好评率、在售数等）")
    public Result<UserPublicVO> getPublicProfile(@PathVariable Long id) {
        UserPublicVO vo = sysUserService.getPublicProfile(id);
        return Result.success(vo);
    }

    // ================== 信用分 ==================

    @GetMapping("/credit")
    @Operation(summary = "获取信用分", description = "获取当前用户的信用分")
    public Result<Integer> getCreditScore() {
        Integer score = sysUserService.getCreditScore();
        return Result.success(score);
    }

    @GetMapping("/credit/logs")
    @Operation(summary = "信用分变更记录", description = "分页获取当前用户的信用分变更历史")
    public Result<Page<CreditLog>> getCreditLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CreditLog> page = sysUserService.getCreditLogs(pageNum, pageSize);
        return Result.success(page);
    }
}
