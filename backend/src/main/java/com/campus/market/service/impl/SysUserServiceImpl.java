package com.campus.market.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.BizException;
import com.campus.market.common.JwtInterceptor;
import com.campus.market.common.JwtUtils;
import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.RegisterDTO;
import com.campus.market.dto.UpdateUserDTO;
import com.campus.market.entity.CreditLog;
import com.campus.market.entity.Evaluation;
import com.campus.market.entity.GoodsInfo;
import com.campus.market.entity.SysUser;
import com.campus.market.entity.TradeOrder;
import com.campus.market.mapper.CreditLogMapper;
import com.campus.market.mapper.EvaluationMapper;
import com.campus.market.mapper.GoodsInfoMapper;
import com.campus.market.mapper.SysUserMapper;
import com.campus.market.mapper.TradeOrderMapper;
import com.campus.market.service.NotificationService;
import com.campus.market.service.SysUserService;
import com.campus.market.util.CryptoUtils;
import com.campus.market.util.PageUtil;
import com.campus.market.vo.LoginVO;
import com.campus.market.vo.UserInfoVO;
import com.campus.market.vo.UserPublicVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 *
 * 核心逻辑：
 * 1. 注册：手机号 AES-GCM 加密存储 + HMAC 盲索引 + BCrypt 密码
 * 2. 登录：通过 phone_hash 或 student_id 查找用户，BCrypt 验证密码，签发 JWT
 * 3. 个人信息：解密手机号并脱敏展示
 * 4. 信用分：变更时同步记录 credit_log（只增不改不删）
 */
@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CreditLogMapper creditLogMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private GoodsInfoMapper goodsInfoMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private NotificationService notificationService;

    @Value("${campus.market.credit.initial}")
    private Integer initialCredit;

    @Value("${campus.market.credit.min}")
    private Integer minCredit;

    @Value("${campus.market.credit.max}")
    private Integer maxCredit;

    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 登录失败锁定：账号 -> 失败信息（连续5次密码错误锁定15分钟） */
    private static final int LOGIN_FAIL_LIMIT = 5;
    private static final long LOGIN_LOCK_MS = 15 * 60 * 1000L;
    private final Map<String, LoginFailInfo> loginFailMap = new ConcurrentHashMap<>();

    /** 登录失败记录 */
    private static class LoginFailInfo {
        int count;
        long lockUntil;
    }

    // ================== 注册 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        log.info("[用户注册] phone={}, nickname={}", dto.getPhone(), dto.getNickname());

        // 1. 检查手机号是否已注册（通过 HMAC 盲索引查询）
        String phoneHash = cryptoUtils.phoneHash(dto.getPhone());
        Long phoneCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhoneHash, phoneHash)
        );
        if (phoneCount > 0) {
            throw new BizException(400, "该手机号已注册");
        }

        // 2. 检查学号是否已存在（如果填了学号）
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            Long studentCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentId, dto.getStudentId())
            );
            if (studentCount > 0) {
                throw new BizException(400, "该学号已被使用");
            }
        }

        // 3. 构建用户实体
        SysUser user = new SysUser();
        user.setNickname(dto.getNickname());
        // 手机号加密存储
        user.setPhone(cryptoUtils.encryptPhone(dto.getPhone()));
        user.setPhoneHash(phoneHash);
        // 密码 BCrypt 加密
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setStudentId(dto.getStudentId());
        user.setRealName(dto.getRealName());
        // 初始值
        user.setCreditScore(initialCredit);  // 100
        user.setRole(0);                      // 普通用户
        user.setCancelStatus(0);              // 正常
        user.setStatus(1);                    // 正常

        // 4. 插入数据库
        sysUserMapper.insert(user);
        log.info("[用户注册成功] userId={}, nickname={}", user.getId(), user.getNickname());

        return user.getId();
    }

    // ================== 登录 ==================

    @Override
    public LoginVO login(LoginDTO dto) {
        log.info("[用户登录] account={}", dto.getAccount());

        // 0. 登录失败锁定检查
        LoginFailInfo failInfo = loginFailMap.get(dto.getAccount());
        if (failInfo != null && failInfo.lockUntil > System.currentTimeMillis()) {
            long remainMin = (failInfo.lockUntil - System.currentTimeMillis()) / 60000;
            throw new BizException(429, "登录失败次数过多，账号已锁定，请" + (remainMin + 1) + "分钟后再试");
        }

        // 1. 判断账号类型：手机号 or 学号
        SysUser user;
        if (PHONE_PATTERN.matcher(dto.getAccount()).matches()) {
            // 手机号登录：通过 HMAC 盲索引查询
            String phoneHash = cryptoUtils.phoneHash(dto.getAccount());
            user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhoneHash, phoneHash)
            );
        } else {
            // 学号登录
            user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentId, dto.getAccount())
            );
        }

        // 2. 用户不存在
        if (user == null) {
            throw new BizException(400, "账号或密码错误");
        }

        // 3. 账号状态检查
        if (user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 密码验证（BCrypt）
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            LoginFailInfo info = loginFailMap.computeIfAbsent(dto.getAccount(), k -> new LoginFailInfo());
            info.count++;
            if (info.count >= LOGIN_FAIL_LIMIT) {
                info.count = 0;
                info.lockUntil = System.currentTimeMillis() + LOGIN_LOCK_MS;
                throw new BizException(429, "密码错误次数过多，账号已锁定15分钟");
            }
            throw new BizException(400, "账号或密码错误");
        }

        // 4.5 登录成功，清除失败记录
        loginFailMap.remove(dto.getAccount());

        // 5. 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getPhone(), user.getRole());

        // 6. 构建返回对象
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setCreditScore(user.getCreditScore());

        log.info("[登录成功] userId={}, role={}", user.getId(), user.getRole());
        return vo;
    }

    // ================== 获取个人信息 ==================

    @Override
    public UserInfoVO getUserInfo() {
        Long userId = JwtInterceptor.getCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());

        // 解密手机号并脱敏
        if (user.getPhone() != null) {
            String decryptedPhone = cryptoUtils.decryptPhone(user.getPhone());
            vo.setPhone(maskPhone(decryptedPhone));
        }

        vo.setStudentId(user.getStudentId());
        vo.setRealName(user.getRealName());
        vo.setQq(user.getQq());
        vo.setWechat(user.getWechat());
        vo.setCreditScore(user.getCreditScore());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null);

        return vo;
    }

    // ================== 修改个人信息 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UpdateUserDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        log.info("[修改个人信息] userId={}", userId);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        // 选择性更新非空字段
        boolean hasUpdate = false;
        if (dto.getNickname() != null && !dto.getNickname().isEmpty()) {
            user.setNickname(dto.getNickname());
            hasUpdate = true;
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
            hasUpdate = true;
        }
        if (dto.getQq() != null) {
            user.setQq(dto.getQq());
            hasUpdate = true;
        }
        if (dto.getWechat() != null) {
            user.setWechat(dto.getWechat());
            hasUpdate = true;
        }
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            // 检查学号是否被其他人使用
            if (!dto.getStudentId().equals(user.getStudentId())) {
                Long count = sysUserMapper.selectCount(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStudentId, dto.getStudentId())
                                .ne(SysUser::getId, userId)
                );
                if (count > 0) {
                    throw new BizException(400, "该学号已被其他用户使用");
                }
                user.setStudentId(dto.getStudentId());
                hasUpdate = true;
            }
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
            hasUpdate = true;
        }

        if (hasUpdate) {
            // @Version 乐观锁：updateById 会自动带上 version 条件
            int rows = sysUserMapper.updateById(user);
            if (rows == 0) {
                throw new BizException(409, "信息更新失败，请刷新后重试（数据已被其他人修改）");
            }
        }
    }

    // ================== 他人公开主页 ==================

    @Override
    public UserPublicVO getPublicProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCreditScore(user.getCreditScore());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null);

        // 在售商品数
        Long onSaleCount = goodsInfoMapper.selectCount(
                new LambdaQueryWrapper<GoodsInfo>()
                        .eq(GoodsInfo::getSellerId, userId)
                        .eq(GoodsInfo::getStatus, 1)
        );
        vo.setOnSaleCount(onSaleCount);

        // 好评率：收到的正常评价中 4-5 星占比
        Long totalReview = evaluationMapper.selectCount(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluateeId, userId)
                        .eq(Evaluation::getStatus, 1)
        );
        if (totalReview > 0) {
            Long goodReview = evaluationMapper.selectCount(
                    new LambdaQueryWrapper<Evaluation>()
                            .eq(Evaluation::getEvaluateeId, userId)
                            .eq(Evaluation::getStatus, 1)
                            .ge(Evaluation::getScore, 4)
            );
            vo.setGoodRate((int) Math.round(goodReview * 100.0 / totalReview));
        } else {
            vo.setGoodRate(100); // 无评价默认满分
        }

        return vo;
    }

    // ================== 信用分 ==================

    @Override
    public Integer getCreditScore() {
        Long userId = JwtInterceptor.getCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user.getCreditScore();
    }

    @Override
    public Page<CreditLog> getCreditLogs(Integer pageNum, Integer pageSize) {
        Long userId = JwtInterceptor.getCurrentUserId();

        Page<CreditLog> page = PageUtil.of(pageNum, pageSize);
        return creditLogMapper.selectPage(page,
                new LambdaQueryWrapper<CreditLog>()
                        .eq(CreditLog::getUserId, userId)
                        .orderByDesc(CreditLog::getCreateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCreditScore(Long userId, int changeValue, String reason, Long orderId, Long operatorId) {
        log.info("[信用分变更] userId={}, change={}, reason={}", userId, changeValue, reason);

        // 1. 查询用户当前信用分
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        int beforeScore = user.getCreditScore();
        int afterScore = beforeScore + changeValue;

        // 2. 信用分范围限制（0-100）
        if (afterScore < minCredit) {
            afterScore = minCredit;
        }
        if (afterScore > maxCredit) {
            afterScore = maxCredit;
        }

        int actualChange = afterScore - beforeScore;

        // 3. 更新用户信用分（乐观锁：updateById 自动带 version 条件）
        user.setCreditScore(afterScore);
        if (afterScore < 10) {
            user.setStatus(0);
            user.setAvatar(null); // 冻结时重置头像
            log.warn("[信用分变更] 信用分低于10，账号自动冻结, userId={}", userId);
        } else if (afterScore >= 10 && beforeScore < 10 && user.getStatus() == 0) {
            // 仅"信用分冻结"（变更前 <10）自动解冻；管理员封禁（信用分 >=10）不在此列
            user.setStatus(1);
        }
        int rows = sysUserMapper.updateById(user);
        if (rows == 0) {
            // 乐观锁冲突，重试一次
            log.warn("[信用分变更] 乐观锁冲突，重试一次, userId={}", userId);
            user = sysUserMapper.selectById(userId);
            if (user == null) {
                throw new BizException(404, "用户不存在");
            }
            beforeScore = user.getCreditScore();
            afterScore = Math.max(minCredit, Math.min(maxCredit, beforeScore + changeValue));
            actualChange = afterScore - beforeScore;
            user.setCreditScore(afterScore);
            if (afterScore < 10) {
                user.setStatus(0);
                user.setAvatar(null); // 冻结时重置头像
                log.warn("[信用分变更] 信用分低于10，账号自动冻结(重试), userId={}", userId);
            } else if (afterScore >= 10 && beforeScore < 10 && user.getStatus() == 0) {
                // 仅"信用分冻结"（变更前 <10）自动解冻；管理员封禁（信用分 >=10）不在此列
                user.setStatus(1);
            }
            rows = sysUserMapper.updateById(user);
            if (rows == 0) {
                throw new BizException(409, "信用分更新失败，请重试");
            }
        }

        // 3.5 冻结副作用：自动下架在售商品 + 通知有进行中订单的买家
        if (afterScore < 10) {
            freezeUserSideEffects(userId);
        }

        // 4. 记录信用分变更日志（只 INSERT，不 UPDATE/DELETE）
        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setBeforeScore(beforeScore);
        creditLog.setAfterScore(afterScore);
        creditLog.setChangeValue(actualChange);
        creditLog.setReason(reason);
        creditLog.setRelatedOrderId(orderId);
        creditLog.setOperatorId(operatorId != null ? operatorId : 0L);
        creditLogMapper.insert(creditLog);

        log.info("[信用分变更完成] userId={}, {} -> {}", userId, beforeScore, afterScore);
    }

    /**
     * 账号冻结副作用：自动下架在售商品，并通知有进行中订单的买家暂停交易
     */
    private void freezeUserSideEffects(Long userId) {
        try {
            // 1. 下架该用户所有在售商品
            goodsInfoMapper.update(null,
                    new LambdaUpdateWrapper<GoodsInfo>()
                            .eq(GoodsInfo::getSellerId, userId)
                            .eq(GoodsInfo::getStatus, 1)
                            .set(GoodsInfo::getStatus, 0)
            );

            // 2. 通知有进行中订单的买家（暂停交易提醒）
            List<TradeOrder> pendingOrders = tradeOrderMapper.selectList(
                    new LambdaQueryWrapper<TradeOrder>()
                            .eq(TradeOrder::getSellerId, userId)
                            .eq(TradeOrder::getStatus, 0)
            );
            for (TradeOrder order : pendingOrders) {
                // relatedId 传 null：type=7 系统通知的前端跳转约定是"商品详情"（relatedId=商品id），
                // 冻结通知的订单 id 会被误当商品 id 跳转错误页面，故不跳转
                notificationService.sendNotification(
                        order.getBuyerId(), 7,
                        "卖家已被冻结",
                        "您交易的卖家账号已被冻结，交易已暂停，商品已下架，请注意交易安全",
                        null
                );
            }
            log.info("[账号冻结副作用完成] userId={}, 已下架在售商品并通知{}个买家", userId, pendingOrders.size());
        } catch (Exception e) {
            log.error("[账号冻结副作用失败] userId={}", userId, e);
        }
    }

    // ================== 工具方法 ==================

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
