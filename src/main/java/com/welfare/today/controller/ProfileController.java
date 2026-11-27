package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.dto.WelfareProfileForm;
import com.welfare.today.service.MemberProfileService;
import com.welfare.today.service.RegionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    public static final String SESSION_PROFILE_KEY = "welfareProfile";

    private final RegionService regionService;
    private final MemberProfileService memberProfileService;

    public ProfileController(RegionService regionService, MemberProfileService memberProfileService) {
        this.regionService = regionService;
        this.memberProfileService = memberProfileService;
    }

    /** 로그인 회원은 계정에 저장된 조건으로, 비회원은 이번 방문 세션의 조건으로 폼을 채운다. */
    @GetMapping
    public String form(@RequestParam(required = false) String welcome,
                       @RequestParam(required = false) String withdrawn,
                       Authentication authentication, HttpSession session, Model model) {
        model.addAttribute("withdrawn", withdrawn != null);
        Optional<String> username = CurrentUser.username(authentication);
        WelfareProfile saved = username.flatMap(memberProfileService::find)
                .orElse((WelfareProfile) session.getAttribute(SESSION_PROFILE_KEY));
        model.addAttribute("welcome", welcome != null);
        return renderForm(WelfareProfileForm.from(saved), username.isPresent(), model);
    }

    @PostMapping
    public String submit(@ModelAttribute WelfareProfileForm profileForm, Authentication authentication,
                         HttpSession session, Model model) {
        Optional<String> username = CurrentUser.username(authentication);
        if (profileForm.hasNoCondition()) {
            model.addAttribute("formError", "생애주기·가구상황·관심주제 중 하나 이상을 고르거나 시도를 선택해 주세요.");
            return renderForm(profileForm, username.isPresent(), model);
        }
        WelfareProfile profile = profileForm.toProfile();
        session.setAttribute(SESSION_PROFILE_KEY, profile);
        username.ifPresent(name -> memberProfileService.save(name, profile));
        return "redirect:/match";
    }

    private String renderForm(WelfareProfileForm profileForm, boolean loggedIn, Model model) {
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("lifeStages", LifeStage.values());
        model.addAttribute("householdSituations", HouseholdSituation.values());
        model.addAttribute("interestThemes", InterestTheme.values());
        model.addAttribute("regions", regionService.regions());
        return "profile-form";
    }
}
