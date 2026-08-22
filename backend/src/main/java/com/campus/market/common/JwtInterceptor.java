package com.campus.market.common;

import com.campus.market.entity.SysUser;
import com.campus.market.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * 拦截所有需要登录的接口，从请求头中提取 token 并验证
 *  - 前端请求 Header 中携带 Authorization: Bearer {token}
 *  - 拦截器验证 token 有效性，将用户信息存入 ThreadLocal 供后续使用
 *  - 管理员接口额外校验 userType = 1
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysUserMapper sysUserMapper;

    /** ThreadLocal 存储当前登录用户ID，方便后续业务代码获取 */
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    /** ThreadLocal 存储当前用户类型 */
    private static final ThreadLocal<Integer> CURRENT_USER_TYPE = new ThreadLocal<>();

    /** 可选认证路径前缀（有 token 就解析用户，没有也不拦截） */
    private static final String[] OPTIONAL_AUTH_PREFIXES = {
        "/goods/list", "/goods/search", "/goods/detail/",
        "/review/goods/"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取 token
        String token = extractToken(request);

        // 2. 无 token 时判断是否为可选认证路径
        if (token == null) {
            if (isOptionalAuth(request.getRequestURI())) {
                return true; // 匿名放行
            }
            throw new BizException(401, "请先登录");
        }

        // 3. 验证 token
        jwtUtils.validateToken(token);

        // 4. 提取用户ID和用户类型
        Long userId = jwtUtils.getUserIdFromToken(token);
        Integer userType = jwtUtils.getUserTypeFromToken(token);

        // 5. 存入 ThreadLocal
        CURRENT_USER.set(userId);
        CURRENT_USER_TYPE.set(userType);

        // 6. 将 userId 放入 request attribute，供后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("userType", userType);

        // 6.5 查用户状态，禁用/冻结则拒绝（防止已禁用用户的旧 token 仍有效）
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null || currentUser.getStatus() == 0) {
            CURRENT_USER.remove();
            CURRENT_USER_TYPE.remove();
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }

        // 7. 管理端接口统一校验管理员权限（/admin/**）
        if (request.getRequestURI().startsWith("/admin/")
                && (userType == null || userType != 1)) {
            CURRENT_USER.remove();
            CURRENT_USER_TYPE.remove();
            throw new BizException(403, "需要管理员权限");
        }

        log.debug("[JWT认证] userId={}, userType={}", userId, userType);
        return true;
    }

    /**
     * 判断请求路径是否为可选认证路径
     */
    private boolean isOptionalAuth(String uri) {
        for (String prefix : OPTIONAL_AUTH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        CURRENT_USER.remove();
        CURRENT_USER_TYPE.remove();
    }

    /**
     * 从请求头中提取 JWT Token
     * 支持格式：Authorization: Bearer {token}
     */
    private String extractToken(HttpServletRequest request) {
        // 仅从请求头取 token：URL 参数传 token 会进入浏览器历史/Referer/日志，造成泄露
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // ================== 静态工具方法 ==================

    public static Long getCurrentUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) {
            throw new BizException(401, "用户未登录");
        }
        return userId;
    }

    /**
     * 获取当前登录用户ID（可选认证场景）
     * 如果用户未登录，返回 null 而非抛出异常
     * 用于商品列表/详情等匿名可访问但登录后体验更好的接口
     */
    public static Long getCurrentUserIdOrNull() {
        return CURRENT_USER.get();
    }

    public static Integer getCurrentUserType() {
        return CURRENT_USER_TYPE.get();
    }

    public static boolean isAdmin() {
        Integer userType = CURRENT_USER_TYPE.get();
        return userType != null && userType == 1;
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BizException(403, "需要管理员权限");
        }
    }
}
