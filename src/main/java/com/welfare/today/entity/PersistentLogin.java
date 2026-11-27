package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * "로그인 상태 유지" 토큰 테이블. 실제 읽기/쓰기는 Spring Security의 JdbcTokenRepositoryImpl이 SQL로 하고,
 * 이 엔티티는 ddl-auto가 같은 스키마로 테이블을 만들게 하는 용도다 — 컬럼 이름/길이를 바꾸면 안 된다.
 */
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {

    @Id
    @Column(name = "series", length = 64)
    private String series;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;

    protected PersistentLogin() {
    }
}
