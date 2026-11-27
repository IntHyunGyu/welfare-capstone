package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.service.MemberProfileService;
import com.welfare.today.service.MemberService;
import com.welfare.today.service.WelfareMatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Controller
@RequestMapping("/match")
public class MatchController {

    private final WelfareMatchService matchService;
    private final MemberProfileService memberProfileService;
    private final MemberService memberService;

    static final String SESSION_VISIT_KEY = "visitSince";

    /**
     * 이번 세션의 "지난 방문" 기준 시각. 세션의 첫 결과 화면에서 한 번 정해 두어, 새로고침해도 안내가 사라지지 않게 한다.
     * 같은 세션에서 다른 아이디로 로그인하는 경우를 구분하려고 아이디를 함께 둔다. since가 null이면 첫 방문.
     */
    record VisitSince(String username, LocalDateTime since) implements Serializable {
    }

    public MatchController(WelfareMatchService matchService, MemberProfileService memberProfileService,
                           MemberService memberService) {
        this.matchService = matchService;
        this.memberProfileService = memberProfileService;
        this.memberService = memberService;
    }

    /**
     * 로그인 회원은 계정에 저장된 조건으로 바로 보여준다. 계정에 조건이 없는데 비회원일 때 고른 조건이
     * 세션에 있으면(조건을 고른 뒤 가입/로그인한 경우) 그 조건을 계정에 저장해 이어받는다.
     */
    @GetMapping
    public String result(Authentication authentication, HttpSession session, Model model) {
        Optional<String> username = CurrentUser.username(authentication);
        WelfareProfile sessionProfile = (WelfareProfile) session.getAttribute(ProfileController.SESSION_PROFILE_KEY);
        WelfareProfile profile = username.flatMap(memberProfileService::find).orElse(null);
        if (profile == null && sessionProfile != null) {
            profile = sessionProfile;
            username.ifPresent(name -> memberProfileService.save(name, sessionProfile));
        }
        if (profile == null) {
            return "redirect:/profile";
        }

        List<WelfareBenefit> benefits = matchService.match(profile);
        model.addAttribute("savedToAccount", username.isPresent());
        model.addAttribute("profile", profile);
        model.addAttribute("conditions", conditionLabels(profile));
        model.addAttribute("benefits", benefits);
        username.ifPresent(name -> addSinceLastVisit(name, benefits, session, model));
        return "match-result";
    }

    /** 지난 방문 이후 처음 들어온(first_seen_at) 혜택 중 내 조건에 맞는 것. 첫 방문이면 아무것도 안 보여준다. */
    private void addSinceLastVisit(String username, List<WelfareBenefit> benefits, HttpSession session, Model model) {
        VisitSince visit = (VisitSince) session.getAttribute(SESSION_VISIT_KEY);
        if (visit == null || !visit.username().equals(username)) {
            visit = new VisitSince(username, memberService.recordVisit(username));
            session.setAttribute(SESSION_VISIT_KEY, visit);
        }
        if (visit.since() == null) {
            return;
        }
        LocalDateTime since = visit.since();
        model.addAttribute("lastVisitAt", since);
        model.addAttribute("newSinceLastVisit", benefits.stream()
                .filter(b -> b.getFirstSeenAt() != null && b.getFirstSeenAt().isAfter(since))
                .toList());
    }

    /** 결과 화면 상단에 "어떤 조건으로 찾았는지" 보여줄 라벨들 (생애주기 → 가구상황 → 관심주제 → 지역). */
    static List<String> conditionLabels(WelfareProfile profile) {
        List<String> labels = new ArrayList<>();
        addLabels(labels, profile.lifeCodes(), LifeStage::labelOfCode);
        addLabels(labels, profile.householdCodes(), HouseholdSituation::labelOfCode);
        addLabels(labels, profile.themeCodes(), InterestTheme::labelOfCode);
        if (profile.ctpvNm() != null && !profile.ctpvNm().isBlank()) {
            boolean hasSgg = profile.sggNm() != null && !profile.sggNm().isBlank();
            labels.add(hasSgg ? profile.ctpvNm() + " " + profile.sggNm() : profile.ctpvNm());
        }
        return labels;
    }

    private static void addLabels(List<String> labels, List<String> codes, UnaryOperator<String> toLabel) {
        if (codes == null) {
            return;
        }
        codes.stream().map(toLabel).filter(Objects::nonNull).forEach(labels::add);
    }
}
