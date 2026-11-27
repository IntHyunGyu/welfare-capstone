package com.welfare.today.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 비회원도 모든 화면을 그대로 쓸 수 있게 permitAll로 두고, 로그인은 회원 전용 기능에만 쓴다.
 * 관리자 페이지(/manage)만 ADMIN 권한이 필요하다 (권한은 MemberService가 ADMIN_USERNAMES로 부여).
 * /admin/** 의 curl용 엔드포인트는 세션이 아니라 X-Admin-Token(AdminTokenInterceptor)으로 보호하므로 CSRF에서 뺀다.
 */
@Configuration
public class SecurityConfig {

    private static final int REMEMBER_ME_SECONDS = 60 * 60 * 24 * 30;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserDetailsService userDetailsService,
                                                   @Value("${welfare.security.remember-me-key}") String rememberMeKey)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/manage/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID", "remember-me"))
                // 키를 고정해야 재배포 후에도 "로그인 상태 유지" 쿠키가 유효하다.
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS))
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/admin/batch/**", "/admin/summaries/**", "/admin/labels", "/h2-console/**"))
                // 로컬 H2 콘솔은 iframe을 쓴다.
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
