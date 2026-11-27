package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.service.WelfareMatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

@Controller
@RequestMapping("/match")
public class MatchController {

    private final WelfareMatchService matchService;

    public MatchController(WelfareMatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public String result(HttpSession session, Model model) {
        WelfareProfile profile = (WelfareProfile) session.getAttribute(ProfileController.SESSION_PROFILE_KEY);
        if (profile == null) {
            return "redirect:/profile";
        }

        List<WelfareBenefit> benefits = matchService.match(profile);
        model.addAttribute("profile", profile);
        model.addAttribute("conditions", conditionLabels(profile));
        model.addAttribute("benefits", benefits);
        return "match-result";
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
