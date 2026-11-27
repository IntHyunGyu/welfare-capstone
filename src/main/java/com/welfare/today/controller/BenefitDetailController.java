package com.welfare.today.controller;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import com.welfare.today.service.WelfareBenefitDetailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
public class BenefitDetailController {

    private final WelfareBenefitRepository benefitRepository;
    private final WelfareBenefitDetailService detailService;

    public BenefitDetailController(WelfareBenefitRepository benefitRepository,
                                   WelfareBenefitDetailService detailService) {
        this.benefitRepository = benefitRepository;
        this.detailService = detailService;
    }

    @GetMapping("/benefits/{servId}")
    public String detail(@PathVariable String servId,
                         @RequestParam(required = false) String from,
                         HttpSession session, Model model) {
        WelfareBenefit benefit = findBenefit(servId);
        model.addAttribute("benefit", benefit);
        model.addAttribute("detail", detailService.getOrFetch(benefit));
        model.addAttribute("summaryEnabled", detailService.summaryEnabled());
        addBackLink(from, session, model);
        return "benefit-detail";
    }

    /**
     * 상세에서 돌아갈 곳. 저장한 혜택에서 왔으면 그쪽으로, 세션에 조건이 있으면 매칭 결과로,
     * 둘 다 아니면(링크를 직접 열었거나 세션 만료) 조건 입력으로 — /match로 보내면 어차피 튕긴다.
     */
    private void addBackLink(String from, HttpSession session, Model model) {
        if ("favorites".equals(from)) {
            model.addAttribute("backUrl", "/favorites");
            model.addAttribute("backLabel", "← 저장한 혜택");
        } else if (session.getAttribute(ProfileController.SESSION_PROFILE_KEY) != null) {
            model.addAttribute("backUrl", "/match");
            model.addAttribute("backLabel", "← 목록으로");
        } else {
            model.addAttribute("backUrl", "/profile");
            model.addAttribute("backLabel", "← 조건 입력");
        }
    }

    /**
     * 상세 화면이 뜬 뒤 스크립트가 호출한다. 요약 생성에 몇 초 걸리므로 화면 렌더링과 분리했다.
     * LLM 비용이 드는 요청이라 브라우저 미리 불러오기나 크롤러가 건드리지 않도록 POST로 둔다.
     */
    @PostMapping("/benefits/{servId}/summary")
    @ResponseBody
    public ResponseEntity<Map<String, String>> summary(@PathVariable String servId) {
        return detailService.getOrCreateSummary(findBenefit(servId))
                .map(summary -> ResponseEntity.ok(Map.of("summary", summary)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private WelfareBenefit findBenefit(String servId) {
        return benefitRepository.findById(servId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
