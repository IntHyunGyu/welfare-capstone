package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 회원. 개인정보를 최소로 두려고 아이디/비밀번호만 받는다 (이메일 수집 안 함).
 */
@Entity
@Table(name = "members")
public class Member {

    public enum Role { USER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 마지막으로 결과 화면을 본 시각 ("지난 방문 이후 새 혜택" 기준). 아직 본 적 없으면 null. */
    @Column(name = "last_visited_at")
    private LocalDateTime lastVisitedAt;

    protected Member() {
    }

    public Member(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 이번 방문을 기록하고 직전 방문 시각을 돌려준다 (처음이면 null). */
    public LocalDateTime recordVisit(LocalDateTime now) {
        LocalDateTime previous = this.lastVisitedAt;
        this.lastVisitedAt = now;
        return previous;
    }
}
