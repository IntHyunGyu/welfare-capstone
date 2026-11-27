package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
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
    public String form(Model model) {
        model.addAttribute("profileForm", new WelfareProfileForm());
        model.addAttribute("lifeStages", LifeStage.values());
        model.addAttribute("householdSituations", HouseholdSituation.values());
        model.addAttribute("interestThemes", InterestTheme.values());
        model.addAttribute("regions", regionService.regions());
        return "profile-form";
    }

    @PostMapping
    public String submit(@ModelAttribute WelfareProfileForm profileForm, HttpSession session) {
        session.setAttribute(SESSION_PROFILE_KEY, profileForm.toProfile());
        return "redirect:/match";
    }
}
