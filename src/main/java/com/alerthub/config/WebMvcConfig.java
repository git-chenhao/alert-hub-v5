package com.alerthub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final DashboardAuthInterceptor dashboardAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // API 认证拦截器 - 保护所有 API 接口
        registry.addInterceptor(apiKeyAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/health", "/api/ready", "/api/live");  // 健康检查端点不需要认证

        // Dashboard 访问控制拦截器
        registry.addInterceptor(dashboardAuthInterceptor)
            .addPathPatterns("/dashboard/**")
            .excludePathPatterns("/dashboard/login", "/dashboard/login/**", "/dashboard/css/**", "/dashboard/js/**");
    }
}
