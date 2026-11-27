package com.welfare.today.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * 비회원도 모든 화면을 그대로 쓸 수 있게 permitAll로 두고, 로그인은 회원 전용 기능에만 쓴다.
 * 관리자 페이지(/manage)만 ADMIN 권한이 필요하다 (권한은 MemberService가 ADMIN_USERNAMES로 부여).
 */
@Configuration
public class SecurityConfig {

    private static final int REMEMBER_ME_SECONDS = 60 * 60 * 24 * 30;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserDetailsService userDetailsService,
                                                   PersistentTokenRepository tokenRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/manage/**").hasRole("ADMIN")
                        .requestMatchers("/mypage/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID", "remember-me"))
                // 토큰을 DB(persistent_logins)에 두는 방식이라 서명 키 환경변수가 필요 없고,
                // 재배포해도 유지되며, 로그아웃하면 그 회원의 토큰이 DB에서 지워진다.
                .rememberMe(remember -> remember
                        .tokenRepository(tokenRepository)
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS))
                // 로컬 H2 콘솔은 자체 폼을 쓰고 iframe으로 뜬다.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }
}
