package com.alerthub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Dashboard 访问控制拦截器
 * 使用简单的密码保护，适合内部管理系统
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHENTICATED_SESSION_KEY = "dashboard_authenticated";

    private final AlertHubProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未配置密码或未启用认证，记录警告并放行
        if (!properties.getSecurity().isDashboardAuthEnabled() ||
            !StringUtils.hasText(properties.getSecurity().getDashboardPassword())) {
            log.warn("Dashboard 未配置访问密码，建议在生产环境配置 DASHBOARD_PASSWORD 环境变量");
            return true;
        }

        HttpSession session = request.getSession(false);

        // 检查是否已认证
        if (session != null && Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED_SESSION_KEY))) {
            return true;
        }

        // 检查是否是登录请求
        if ("POST".equals(request.getMethod()) && "/dashboard/login".equals(request.getRequestURI())) {
            return true;
        }

        // 对于登录页面本身，放行
        if ("/dashboard/login".equals(request.getRequestURI())) {
            return true;
        }

        // 未认证，重定向到登录页面
        log.warn("Dashboard 未授权访问: uri={}, remoteAddr={}", request.getRequestURI(), request.getRemoteAddr());
        response.sendRedirect("/dashboard/login");
        return false;
    }
}
