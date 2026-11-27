package com.welfare.today.controller;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import com.welfare.today.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 저장한 혜택 화면. 로그인 회원은 DB의 즐겨찾기를 바로 그린다.
 * 비회원의 목록은 브라우저 localStorage에만 있어서, ids 파라미터 없이 열리면 화면의 스크립트가
 * 저장된 servId 목록을 붙여 다시 요청한다.
 */
@Controller
public class FavoritesController {

    private static final int MAX_IDS = 100;

    private final WelfareBenefitRepository benefitRepository;
    private final FavoriteService favoriteService;

    public FavoritesController(WelfareBenefitRepository benefitRepository, FavoriteService favoriteService) {
        this.benefitRepository = benefitRepository;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/favorites")
    public String favorites(@RequestParam(required = false) String ids, Authentication authentication,
                            Model model) {
        Optional<String> username = CurrentUser.username(authentication);
        model.addAttribute("savedToAccount", username.isPresent());
        if (username.isPresent()) {
            model.addAttribute("benefits", favoriteService.benefits(username.get()));
        } else if (ids != null) {
            model.addAttribute("benefits", findInOrder(ids));
        }
        return "favorites";
    }

    private List<WelfareBenefit> findInOrder(String ids) {
        List<String> requested = Arrays.stream(ids.split(","))
                .map(String::strip)
                .filter(id -> !id.isEmpty())
                .distinct()
                .limit(MAX_IDS)
                .toList();
        Map<String, WelfareBenefit> byId = benefitRepository.findAllById(requested).stream()
                .collect(Collectors.toMap(WelfareBenefit::getServId, Function.identity()));
        return requested.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
