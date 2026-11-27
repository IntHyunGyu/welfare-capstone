package com.welfare.today.controller;

import com.welfare.today.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void anonymousVisitorCanStillUseTheSite() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("로그인")))
                .andExpect(content().string(not(containsString("로그아웃"))));
    }

    @Test
    void signUpStoresHashedPasswordAndLogsIn() throws Exception {
        HttpSession session = signUp("newuser1", "password123", "password123")
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);

        assertThat(memberRepository.findByUsername("newuser1"))
                .hasValueSatisfying(member -> assertThat(member.getPasswordHash())
                        .startsWith("{bcrypt}")
                        .doesNotContain("password123"));

        mockMvc.perform(get("/profile").session((MockHttpSession) session))
                .andExpect(content().string(containsString("newuser1")))
                .andExpect(content().string(containsString("로그아웃")));
    }

    @Test
    void invalidSignUpShowsReasonAndCreatesNoMember() throws Exception {
        signUp("AB", "short", "short")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("영문 소문자, 숫자, 밑줄(_)로 4~20자")))
                .andExpect(content().string(containsString("8~64자")));
        signUp("gooduser", "password123", "different123")
                .andExpect(content().string(containsString("비밀번호 확인이 일치하지 않아요")));

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        signUp("taken_id", "password123", "password123");
        signUp("taken_id", "password123", "password123")
                .andExpect(content().string(containsString("이미 사용 중인 아이디")));
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void loginSucceedsWithRightPasswordAndFailsOtherwise() throws Exception {
        signUp("loginuser", "password123", "password123");

        mockMvc.perform(formLogin("/login").user("loginuser").password("password123"))
                .andExpect(authenticated().withUsername("loginuser"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin("/login").user("loginuser").password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void rememberMeSetsLongLivedCookie() throws Exception {
        signUp("keepuser", "password123", "password123");

        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "keepuser")
                        .param("password", "password123")
                        .param("remember-me", "on"))
                .andExpect(cookie().exists("remember-me"))
                .andReturn();
        assertThat(result.getResponse().getCookie("remember-me").getMaxAge()).isEqualTo(60 * 60 * 24 * 30);
    }

    @Test
    void scriptPostsNeedCsrfTokenButAdminCurlEndpointsDoNot() throws Exception {
        mockMvc.perform(post("/benefits/NOPE/summary")).andExpect(status().isForbidden());
        mockMvc.perform(post("/benefits/NOPE/summary").with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/signup").param("username", "x")).andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/summaries/reset")).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions signUp(String username, String password,
                                                                       String confirm) throws Exception {
        return mockMvc.perform(post("/signup").with(csrf())
                .param("username", username)
                .param("password", password)
                .param("passwordConfirm", confirm));
    }
}
