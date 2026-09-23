package com.welfare.today.controller;

import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.service.WelfareMatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
        model.addAttribute("benefits", benefits);
        return "match-result";
    }
}
