package com.welfare.today.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/** 컨트롤러에서 "로그인한 회원의 아이디"를 꺼내는 공통 헬퍼. 비회원이면 빈 값. */
final class CurrentUser {

    private CurrentUser() {
    }

    static Optional<String> username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication.getName());
    }
}
