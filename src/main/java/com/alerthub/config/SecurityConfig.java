package com.alerthub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置
 * 保护管理界面，仅允许认证用户访问
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${alerthub.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${alerthub.security.admin-username:admin}")
    private String adminUsername;

    @Value("${alerthub.security.admin-password:}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            http.securityMatcher("/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        http
            .authorizeHttpRequests(auth -> auth
                // API 端点允许匿名访问（webhook 接收告警）
                .requestMatchers("/api/webhook/**").permitAll()
                .requestMatchers("/api/alerts").permitAll()
                // 静态资源允许访问
                .requestMatchers("/static/**").permitAll()
                // H2 控制台（仅开发环境）
                .requestMatchers("/h2-console/**").permitAll()
                // 健康检查
                .requestMatchers("/actuator/**").permitAll()
                // 管理界面需要认证
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/admin", true)
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/h2-console/**")
            )
            // 允许 H2 控制台的 iframe
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String password = adminPassword;
        if (password == null || password.isEmpty()) {
            // 如果没有配置密码，生成一个随机密码并打印到日志
            password = java.util.UUID.randomUUID().toString().substring(0, 16);
            System.out.println("========================================");
            System.out.println("Generated Admin Password: " + password);
            System.out.println("Please set ALERTHUB_ADMIN_PASSWORD environment variable!");
            System.out.println("========================================");
        }

        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(passwordEncoder().encode(password))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
