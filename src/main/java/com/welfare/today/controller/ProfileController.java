package com.welfare.today.controller;

import com.welfare.today.code.HouseholdSituation;
import com.welfare.today.code.InterestTheme;
import com.welfare.today.code.LifeStage;
import com.welfare.today.dto.WelfareProfileForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    public static final String SESSION_PROFILE_KEY = "welfareProfile";

    private static final List<String> SIDO_LIST = List.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시",
            "대전광역시", "울산광역시", "세종특별자치시", "경기도", "강원특별자치도",
            "충청북도", "충청남도", "전북특별자치도", "전라남도", "경상북도",
            "경상남도", "제주특별자치도"
    );

    @GetMapping
    public String form(Model model) {
        model.addAttribute("profileForm", new WelfareProfileForm());
        model.addAttribute("lifeStages", LifeStage.values());
        model.addAttribute("householdSituations", HouseholdSituation.values());
        model.addAttribute("interestThemes", InterestTheme.values());
        model.addAttribute("sidoList", SIDO_LIST);
        return "profile-form";
    }

    @PostMapping
    public String submit(@ModelAttribute WelfareProfileForm profileForm, HttpSession session) {
        session.setAttribute(SESSION_PROFILE_KEY, profileForm.toProfile());
        return "redirect:/match";
    }
}
