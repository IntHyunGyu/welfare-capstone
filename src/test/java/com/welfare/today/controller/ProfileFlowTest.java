package com.welfare.today.controller;

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
class ProfileFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reopeningFormKeepsPreviousChoices() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/profile").session(session).with(csrf())
                        .param("lifeCodes", "004")
                        .param("householdCodes", "050")
                        .param("age", "27")
                        .param("ctpvNm", "서울특별시")
                        .param("sggNm", "송파구"))
                .andExpect(redirectedUrl("/match"));

        String html = mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("value=\"004\" checked=\"checked\"")
                .contains("value=\"050\" checked=\"checked\"")
                .doesNotContain("value=\"001\" checked=\"checked\"")
                .contains("value=\"27\"")
                .contains("data-selected=\"송파구\"");
    }

    @Test
    void firstVisitShowsEmptyForm() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("checked=\"checked\""))));
    }

    @Test
    void submittingWithoutAnyConditionIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/profile").session(session).with(csrf()).param("age", "40"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("하나 이상을 고르거나 시도를 선택해 주세요")))
                .andExpect(content().string(containsString("value=\"40\"")));

        assertThat(session.getAttribute(ProfileController.SESSION_PROFILE_KEY)).isNull();
    }

    @Test
    void regionAloneIsEnough() throws Exception {
        mockMvc.perform(post("/profile").with(csrf()).param("ctpvNm", "서울특별시"))
                .andExpect(redirectedUrl("/match"));
    }
}
