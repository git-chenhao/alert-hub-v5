package com.alerthub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API Key 认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PARAM = "apiKey";

    private final AlertHubProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未配置 API Key 或未启用，则跳过验证
        if (!properties.getSecurity().isApiKeyEnabled() ||
            !StringUtils.hasText(properties.getSecurity().getApiKey())) {
            return true;
        }

        // 从 Header 或参数获取 API Key
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(providedApiKey)) {
            providedApiKey = request.getParameter(API_KEY_PARAM);
        }

        // 验证 API Key
        String expectedApiKey = properties.getSecurity().getApiKey();
        if (!expectedApiKey.equals(providedApiKey)) {
            log.warn("API Key 验证失败: uri={}, remoteAddr={}", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API Key\"}");
            return false;
        }

        return true;
    }
}
