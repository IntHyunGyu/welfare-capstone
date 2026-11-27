package com.welfare.today.controller;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.FavoriteRepository;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class FavoriteFlowTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WelfareBenefitRepository benefitRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        benefitRepository.saveAll(List.of(benefit("FAV1", "첫째 혜택"), benefit("FAV2", "둘째 혜택")));
        session = new MockHttpSession();
        mockMvc.perform(post("/signup").session(session).with(csrf())
                .param("username", "fav_user").param("password", PASSWORD).param("passwordConfirm", PASSWORD));
    }

    @Test
    void apiRequiresLoginAndCsrf() throws Exception {
        mockMvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/favorites/FAV1").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/favorites/FAV1").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void saveAndRemoveAreRepeatableAndUnknownBenefitIsRejected() throws Exception {
        mockMvc.perform(post("/api/favorites/FAV1").session(session).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/favorites/FAV1").session(session).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/favorites/FAV2").session(session).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/favorites/NOPE").session(session).with(csrf())).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/favorites").session(session))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("FAV2"))
                .andExpect(jsonPath("$[1]").value("FAV1"));

        mockMvc.perform(delete("/api/favorites/FAV1").session(session).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/favorites/FAV1").session(session).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/favorites").session(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("FAV2"));
    }

    @Test
    void favoritesPageRendersAccountFavoritesNewestFirst() throws Exception {
        mockMvc.perform(post("/api/favorites/FAV1").session(session).with(csrf()));
        mockMvc.perform(post("/api/favorites/FAV2").session(session).with(csrf()));

        String html = mockMvc.perform(get("/favorites").session(session))
                .andExpect(content().string(containsString("내 계정에 저장돼요")))
                .andExpect(content().string(containsString("name=\"fav-api\"")))
                .andExpect(content().string(not(containsString("data-fav-load"))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.indexOf("둘째 혜택")).isLessThan(html.indexOf("첫째 혜택"));

        mockMvc.perform(get("/favorites"))
                .andExpect(content().string(not(containsString("name=\"fav-api\""))))
                .andExpect(content().string(containsString("data-fav-load")));
    }

    @Test
    void withdrawalAlsoDeletesFavorites() throws Exception {
        mockMvc.perform(post("/api/favorites/FAV1").session(session).with(csrf()));
        assertThat(favoriteRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/mypage/withdraw").session(session).with(csrf())
                .param("currentPassword", PASSWORD).param("agree", "on"));
        assertThat(favoriteRepository.count()).isZero();
    }

    private static WelfareBenefit benefit(String id, String name) {
        return new WelfareBenefit(id, name, null, "서울특별시", null, null, null, null, null,
                null, "20261101", "청년", "저소득", "주거", LocalDateTime.now());
    }
}
