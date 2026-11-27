package com.welfare.today.controller;

import com.welfare.today.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 로그인 회원의 저장 버튼이 쓰는 API (favorites.js). 로그인은 SecurityConfig가 /api/** 에 요구하고,
 * 비로그인이면 로그인 화면으로 보내지 않고 401을 돌려준다.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    public FavoriteApiController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<String> list(Authentication authentication) {
        return favoriteService.servIds(authentication.getName());
    }

    /** 비회원 때 이 기기에 저장한 servId 목록(오래된 것부터)을 계정으로 옮긴다. */
    @PostMapping("/import")
    public Map<String, Integer> importAll(@RequestBody List<String> servIds, Authentication authentication) {
        return Map.of("imported", favoriteService.importAll(authentication.getName(), servIds));
    }

    @PostMapping("/{servId}")
    public ResponseEntity<Void> add(@PathVariable String servId, Authentication authentication) {
        return favoriteService.add(authentication.getName(), servId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{servId}")
    public ResponseEntity<Void> remove(@PathVariable String servId, Authentication authentication) {
        favoriteService.remove(authentication.getName(), servId);
        return ResponseEntity.noContent().build();
    }
}
