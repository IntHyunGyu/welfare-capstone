package com.welfare.today.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** 로그인 회원은 저장된 조건으로 바로 결과를 본다 (조건이 없으면 /match가 조건 입력으로 보낸다). */
    @GetMapping("/")
    public String home(Authentication authentication) {
        return CurrentUser.username(authentication).isPresent() ? "redirect:/match" : "redirect:/profile";
    }
}
