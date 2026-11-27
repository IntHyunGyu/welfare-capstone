package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.dto.WelfareProfileForm;
import com.welfare.today.service.RegionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    public static final String SESSION_PROFILE_KEY = "welfareProfile";

    private final RegionService regionService;

    public ProfileController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public String form(HttpSession session, Model model) {
        WelfareProfile saved = (WelfareProfile) session.getAttribute(SESSION_PROFILE_KEY);
        return renderForm(WelfareProfileForm.from(saved), model);
    }

    @PostMapping
    public String submit(@ModelAttribute WelfareProfileForm profileForm, HttpSession session, Model model) {
        if (profileForm.hasNoCondition()) {
            model.addAttribute("formError", "생애주기·가구상황·관심주제 중 하나 이상을 고르거나 시도를 선택해 주세요.");
            return renderForm(profileForm, model);
        }
        session.setAttribute(SESSION_PROFILE_KEY, profileForm.toProfile());
        return "redirect:/match";
    }

    private String renderForm(WelfareProfileForm profileForm, Model model) {
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("lifeStages", LifeStage.values());
        model.addAttribute("householdSituations", HouseholdSituation.values());
        model.addAttribute("interestThemes", InterestTheme.values());
        model.addAttribute("regions", regionService.regions());
        return "profile-form";
    }
}
