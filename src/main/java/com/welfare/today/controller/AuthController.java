package com.welfare.today.controller;

import com.welfare.today.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AuthController {

    private final MemberService memberService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/login")
    public String loginForm(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/" : "login";
    }

    @GetMapping("/signup")
    public String signUpForm(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/" : "signup";
    }

    @PostMapping("/signup")
    public String signUp(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String passwordConfirm,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         Model model) {
        List<String> errors = memberService.validateSignUp(username, password, passwordConfirm);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("username", username);
            return "signup";
        }
        memberService.signUp(username, password);
        logIn(username, request, response);
        return "redirect:/";
    }

    /** 가입 직후 바로 로그인 상태로 만든다. 세션 고정 공격을 막으려고 세션 ID를 바꾼 뒤 저장한다. */
    private void logIn(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails user = memberService.loadUserByUsername(username);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        securityContextRepository.saveContext(context, request, response);
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
