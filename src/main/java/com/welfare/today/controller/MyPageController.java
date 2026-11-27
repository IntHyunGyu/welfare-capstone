package com.welfare.today.controller;

import com.welfare.today.entity.Member;
import com.welfare.today.service.MemberProfileService;
import com.welfare.today.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 마이페이지: 계정 정보, 저장된 조건, 비밀번호 변경, 회원 탈퇴. 로그인은 SecurityConfig가 /mypage/** 에 요구한다. */
@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;

    public MyPageController(MemberService memberService, MemberProfileService memberProfileService) {
        this.memberService = memberService;
        this.memberProfileService = memberProfileService;
    }

    @GetMapping
    public String myPage(Authentication authentication, Model model) {
        return render(authentication.getName(), model);
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String newPasswordConfirm,
                                 Authentication authentication, Model model, RedirectAttributes redirect) {
        String username = authentication.getName();
        List<String> errors = memberService.validatePasswordChange(username, currentPassword, newPassword,
                newPasswordConfirm);
        if (!errors.isEmpty()) {
            model.addAttribute("passwordErrors", errors);
            return render(username, model);
        }
        memberService.changePassword(username, newPassword);
        redirect.addFlashAttribute("passwordMessage",
                "비밀번호를 바꿨어요. 다른 기기의 \"로그인 상태 유지\"는 해제됐어요.");
        return "redirect:/mypage";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam(required = false) String currentPassword,
                           @RequestParam(required = false) String agree,
                           Authentication authentication, Model model,
                           HttpServletRequest request, HttpServletResponse response) {
        String username = authentication.getName();
        if (agree == null) {
            model.addAttribute("withdrawError", "탈퇴 안내를 확인했다는 항목에 체크해 주세요.");
            return render(username, model);
        }
        if (!memberService.passwordMatches(username, currentPassword)) {
            model.addAttribute("withdrawError", "비밀번호가 맞지 않아요.");
            return render(username, model);
        }
        memberService.withdraw(username);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        Cookie rememberMe = new Cookie("remember-me", null);
        rememberMe.setMaxAge(0);
        rememberMe.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        response.addCookie(rememberMe);
        return "redirect:/profile?withdrawn";
    }

    private String render(String username, Model model) {
        Member member = memberService.get(username);
        model.addAttribute("member", member);
        model.addAttribute("conditions", memberProfileService.find(username)
                .map(MatchController::conditionLabels)
                .orElse(List.of()));
        return "mypage";
    }
}
