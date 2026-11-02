package com.educheck.common;

import com.educheck.config.CacheService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * JWT Token 上下文工具
 * 从当前请求的 Authorization 头中解析出用户 ID
 * 所有 Controller 通过此工具获取当前登录用户，禁止硬编码 userId
 */
@Component
@RequiredArgsConstructor
public class TokenContextHolder {

    private final JwtUtil jwtUtil;
    private final CacheService cacheService;

    /**
     * 获取当前请求的原始 token 字符串
     */
    public String getCurrentToken() {
        return getTokenFromRequest();
    }

    /**
     * 获取当前登录用户的 ID
     * @return 用户ID，未登录或 token 无效时返回 null
     */
    public Long getCurrentUserId() {
        String token = getTokenFromRequest();
        if (token == null || token.isEmpty()) {
            return null;
        }
        // 检查 token 是否在黑名单中（已登出）
        if (isTokenBlacklisted(token)) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查 token 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenHash = "token:blacklist:" + token.hashCode();
        return cacheService.hasKey(tokenHash);
    }

    /**
     * 将 token 加入黑名单（登出时调用）
     */
    public void blacklistCurrentToken() {
        String token = getTokenFromRequest();
        if (token == null) return;
        try {
            long ttl = jwtUtil.getExpirationFromToken(token).getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                cacheService.set("token:blacklist:" + token.hashCode(), "1", ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}
    }

    /**
     * 获取当前登录用户的角色
     * @return 角色字符串，未登录或 token 无效时返回 null
     */
    public String getCurrentRole() {
        String token = getTokenFromRequest();
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (isTokenBlacklisted(token)) {
            return null;
        }
        try {
            return jwtUtil.getRoleFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从当前请求中提取 Authorization 头
     */
    private String getTokenFromRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        // 优先从 Header 取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (authHeader != null && !authHeader.isEmpty()) {
            return authHeader;
        }
        // 兼容 URL 参数 ?token=xxx（用于文件上传等场景）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        return null;
    }

    /**
     * 获取当前用户 ID，并校验是否已登录
     * @throws ServiceException 401 未登录
     */
    public Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(401, "未登录或 token 已失效");
        }
        return userId;
    }
}
