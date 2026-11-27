package com.welfare.today.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** 로그인 회원의 즐겨찾기 (비회원은 브라우저 localStorage에만 저장). */
@Entity
@Table(name = "favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite_member_serv", columnNames = {"member_id", "serv_id"}),
        indexes = @Index(name = "idx_favorite_member", columnList = "member_id"))
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "serv_id", nullable = false, length = 20)
    private String servId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Favorite() {
    }

    public Favorite(Long memberId, String servId) {
        this.memberId = memberId;
        this.servId = servId;
        this.createdAt = LocalDateTime.now();
    }

    public String getServId() {
        return servId;
    }
}
