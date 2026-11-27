package com.welfare.today.controller;

import com.welfare.today.repository.MemberProfileRepository;
import com.welfare.today.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class MyPageFlowTest {

    private static final String USER = "my_user";
    private static final String PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newpassword456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository profileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession session;

    @BeforeEach
    void signUp() throws Exception {
        session = new MockHttpSession();
        mockMvc.perform(post("/signup").session(session).with(csrf())
                .param("username", USER).param("password", PASSWORD).param("passwordConfirm", PASSWORD));
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/mypage")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void showsAccountAndSavedConditionsWithHeaderLink() throws Exception {
        mockMvc.perform(get("/mypage").session(session))
                .andExpect(content().string(containsString(USER + "님 · ")))
                .andExpect(content().string(containsString("아직 저장된 조건이 없어요")));

        mockMvc.perform(post("/profile").session(session).with(csrf()).param("lifeCodes", "004"));
        mockMvc.perform(get("/mypage").session(session))
                .andExpect(content().string(containsString(">청년<")))
                .andExpect(content().string(containsString("조건 변경하기")));
        mockMvc.perform(get("/profile").session(session))
                .andExpect(content().string(containsString("href=\"/mypage\"")));
    }

    @Test
    void passwordChangeValidatesThenSwitchesPasswordAndDropsRememberMe() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                .param("username", USER).param("password", PASSWORD).param("remember-me", "on"));
        assertThat(rememberMeTokens()).isEqualTo(1);

        changePassword("wrong-current", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(content().string(containsString("현재 비밀번호가 맞지 않아요")));
        changePassword(PASSWORD, NEW_PASSWORD, "different456")
                .andExpect(content().string(containsString("새 비밀번호 확인이 일치하지 않아요")));
        changePassword(PASSWORD, PASSWORD, PASSWORD)
                .andExpect(content().string(containsString("지금 쓰는 비밀번호와 다른")));

        changePassword(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(redirectedUrl("/mypage"))
                .andExpect(flash().attribute("passwordMessage",
                        "비밀번호를 바꿨어요. 다른 기기의 \"로그인 상태 유지\"는 해제됐어요."));
        assertThat(rememberMeTokens()).isZero();

        mockMvc.perform(formLogin("/login").user(USER).password(PASSWORD)).andExpect(unauthenticated());
        mockMvc.perform(formLogin("/login").user(USER).password(NEW_PASSWORD)).andExpect(authenticated());
    }

    @Test
    void withdrawNeedsAgreementAndPasswordThenDeletesEverythingAndLogsOut() throws Exception {
        mockMvc.perform(post("/profile").session(session).with(csrf()).param("lifeCodes", "004"));
        mockMvc.perform(post("/login").with(csrf())
                .param("username", USER).param("password", PASSWORD).param("remember-me", "on"));
        Long memberId = memberRepository.findByUsername(USER).orElseThrow().getId();
        assertThat(profileRepository.existsById(memberId)).isTrue();

        mockMvc.perform(post("/mypage/withdraw").session(session).with(csrf()).param("currentPassword", PASSWORD))
                .andExpect(content().string(containsString("항목에 체크해 주세요")));
        mockMvc.perform(post("/mypage/withdraw").session(session).with(csrf())
                        .param("currentPassword", "wrong").param("agree", "on"))
                .andExpect(content().string(containsString("비밀번호가 맞지 않아요")));
        assertThat(memberRepository.existsByUsername(USER)).isTrue();

        mockMvc.perform(post("/mypage/withdraw").session(session).with(csrf())
                        .param("currentPassword", PASSWORD).param("agree", "on"))
                .andExpect(redirectedUrl("/profile?withdrawn"));

        assertThat(memberRepository.existsByUsername(USER)).isFalse();
        assertThat(profileRepository.existsById(memberId)).isFalse();
        assertThat(rememberMeTokens()).isZero();
        mockMvc.perform(get("/mypage").session(session)).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(formLogin("/login").user(USER).password(PASSWORD)).andExpect(unauthenticated());
        mockMvc.perform(get("/profile").param("withdrawn", ""))
                .andExpect(content().string(containsString("탈퇴가 끝났어요")));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String current, String next,
                                                                               String confirm) throws Exception {
        return mockMvc.perform(post("/mypage/password").session(session).with(csrf())
                .param("currentPassword", current)
                .param("newPassword", next)
                .param("newPasswordConfirm", confirm));
    }

    private Integer rememberMeTokens() {
        return jdbcTemplate.queryForObject("select count(*) from persistent_logins where username = ?",
                Integer.class, USER);
    }
}
