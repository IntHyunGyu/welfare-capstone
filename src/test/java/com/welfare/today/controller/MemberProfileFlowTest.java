package com.welfare.today.controller;

import com.welfare.today.service.MemberProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class MemberProfileFlowTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberProfileService memberProfileService;

    @Test
    void signUpWithoutConditionsLeadsToFormWithWelcome() throws Exception {
        MockHttpSession session = new MockHttpSession();
        signUp(session, "fresh_user").andExpect(redirectedUrl("/profile?welcome"));

        mockMvc.perform(get("/profile").param("welcome", "").session(session))
                .andExpect(content().string(containsString("가입을 환영해요")))
                .andExpect(content().string(containsString("이 조건은 내 계정에 저장돼서")));
    }

    @Test
    void conditionsChosenBeforeSignUpAreCarriedIntoTheAccount() throws Exception {
        MockHttpSession session = new MockHttpSession();
        submitConditions(session, "004");

        signUp(session, "carry_user").andExpect(redirectedUrl("/match"));
        mockMvc.perform(get("/match").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("내 저장된 조건")));

        assertThat(memberProfileService.find("carry_user"))
                .hasValueSatisfying(profile -> assertThat(profile.lifeCodes()).containsExactly("004"));
    }

    @Test
    void savedConditionsAreUsedOnNextLoginWithoutReentering() throws Exception {
        MockHttpSession first = new MockHttpSession();
        signUp(first, "return_user");
        submitConditions(first, "006");

        // 다른 브라우저/다음 방문 — 세션에 조건이 없어도 로그인만 하면 저장된 조건으로 바로 본다.
        MockHttpSession next = logIn("return_user");
        mockMvc.perform(get("/").session(next)).andExpect(redirectedUrl("/match"));
        mockMvc.perform(get("/match").session(next))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("내 저장된 조건")))
                .andExpect(content().string(containsString("노년")));
        mockMvc.perform(get("/profile").session(next))
                .andExpect(content().string(containsString("value=\"006\" checked=\"checked\"")));
    }

    @Test
    void loggedInWithoutSavedConditionsIsSentToForm() throws Exception {
        signUp(new MockHttpSession(), "empty_user");
        mockMvc.perform(get("/match").session(logIn("empty_user"))).andExpect(redirectedUrl("/profile"));
    }

    @Test
    void anonymousVisitorsStillUseSessionOnly() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/profile"));
        MockHttpSession session = new MockHttpSession();
        submitConditions(session, "004");
        mockMvc.perform(get("/match").session(session))
                .andExpect(content().string(containsString("찾은 조건")))
                .andExpect(content().string(not(containsString("내 저장된 조건"))));
    }

    private org.springframework.test.web.servlet.ResultActions signUp(MockHttpSession session, String username)
            throws Exception {
        return mockMvc.perform(post("/signup").session(session).with(csrf())
                .param("username", username)
                .param("password", PASSWORD)
                .param("passwordConfirm", PASSWORD));
    }

    private void submitConditions(MockHttpSession session, String lifeCode) throws Exception {
        mockMvc.perform(post("/profile").session(session).with(csrf()).param("lifeCodes", lifeCode))
                .andExpect(redirectedUrl("/match"));
    }

    private MockHttpSession logIn(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin("/login").user(username).password(PASSWORD))
                .andReturn().getRequest().getSession(false);
    }
}
