package com.welfare.today.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /admin/** 요청은 X-Admin-Token 헤더가 welfare.admin.token과 같아야 통과한다.
 * 토큰이 비어 있으면(로컬 개발) 검사하지 않는다 - prod 프로필은 ADMIN_TOKEN이 없으면 기동 자체가 실패한다.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Admin-Token";

    private final byte[] token;

    public AdminTokenInterceptor(@Value("${welfare.admin.token:}") String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (token.length == 0) {
            return true;
        }
        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(token, provided.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return false;
    }
}
