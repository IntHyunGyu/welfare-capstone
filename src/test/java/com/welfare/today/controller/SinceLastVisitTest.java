package com.welfare.today.controller;

import com.welfare.today.entity.Member;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.MemberRepository;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class SinceLastVisitTest {

    private static final String USER = "visit_user";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WelfareBenefitRepository benefitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void showsBenefitsThatArrivedSinceThePreviousVisitAndMatchMyConditions() throws Exception {
        benefitRepository.save(benefit("OLD", "예전부터 있던 청년 혜택", "청년", null));
        MockHttpSession first = new MockHttpSession();
        mockMvc.perform(post("/signup").session(first).with(csrf())
                .param("username", USER).param("password", PASSWORD).param("passwordConfirm", PASSWORD));
        mockMvc.perform(post("/profile").session(first).with(csrf()).param("lifeCodes", "004"));

        // 1) 첫 방문: 비교할 기준이 없어 안내하지 않고, 방문 시각만 기록한다
        mockMvc.perform(get("/match").session(first))
                .andExpect(content().string(not(containsString("지난 방문"))));
        LocalDateTime firstVisit = lastVisitedAt();
        assertThat(firstVisit).isNotNull();

        // 2) 그 뒤에 새 혜택이 들어온다 — 하나는 내 조건(청년)에 맞고 하나는 안 맞는다
        benefitRepository.save(benefit("FRESH", "새로 생긴 청년 월세 지원", "청년", firstVisit.plusNanos(1_000)));
        benefitRepository.save(benefit("OTHER", "새로 생긴 노년 혜택", "노년", firstVisit.plusNanos(1_000)));

        // 3) 다음 방문(새로 로그인): 지난 방문 이후 내 조건에 맞는 새 혜택만 보여준다
        MockHttpSession second = logIn();
        String html = mockMvc.perform(get("/match").session(second))
                .andExpect(content().string(containsString("이후 내 조건에 새로 맞는 혜택")))
                .andReturn().getResponse().getContentAsString();
        String panel = html.substring(html.indexOf("class=\"since-visit\""), html.indexOf("</section>"));
        assertThat(panel).contains("새로 생긴 청년 월세 지원").contains("1건")
                .doesNotContain("새로 생긴 노년 혜택").doesNotContain("예전부터 있던 청년 혜택");

        // 4) 같은 세션에서 새로고침해도 안내가 유지된다
        mockMvc.perform(get("/match").session(second))
                .andExpect(content().string(containsString("새로 생긴 청년 월세 지원")))
                .andExpect(content().string(containsString("이후 내 조건에 새로 맞는 혜택")));

        // 5) 그다음 방문: 기준이 직전 방문으로 옮겨져 더 이상 새 혜택이 아니다
        mockMvc.perform(get("/match").session(logIn()))
                .andExpect(content().string(containsString("새로 맞는 혜택은 없어요")));
    }

    @Test
    void anonymousVisitorsSeeNoVisitPanel() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/profile").session(session).with(csrf()).param("lifeCodes", "004"));
        mockMvc.perform(get("/match").session(session))
                .andExpect(content().string(not(containsString("since-visit"))));
    }

    private LocalDateTime lastVisitedAt() {
        Member member = memberRepository.findByUsername(USER).orElseThrow();
        return (LocalDateTime) ReflectionTestUtils.getField(member, "lastVisitedAt");
    }

    private MockHttpSession logIn() throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin("/login").user(USER).password(PASSWORD))
                .andReturn().getRequest().getSession(false);
    }

    private static WelfareBenefit benefit(String id, String name, String life, LocalDateTime firstSeenAt) {
        WelfareBenefit benefit = new WelfareBenefit(id, name, null, "서울특별시", null, null, null, null, null,
                null, "20261101", life, "저소득", "주거", LocalDateTime.now());
        benefit.markFirstSeen(firstSeenAt);
        return benefit;
    }
}
