package com.welfare.today.controller;

import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.entity.DailyMetric;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.entity.WelfareBenefitDetail;
import com.welfare.today.repository.DailyMetricRepository;
import com.welfare.today.repository.WelfareBenefitDetailRepository;
import com.welfare.today.repository.WelfareBenefitRepository;
import com.welfare.today.service.MemberService;
import com.welfare.today.service.WelfareBenefitBatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "welfare.admin.usernames=boss_admin")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class ManageAccessTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DailyMetricRepository metricRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WelfareBenefitRepository benefitRepository;

    @Autowired
    private WelfareBenefitDetailRepository detailRepository;

    @MockBean
    private WelfareBenefitBatchService batchService;

    @BeforeEach
    void setUp() {
        memberService.signUp("boss_admin", PASSWORD);
        memberService.signUp("plain_user", PASSWORD);
    }

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/manage")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void memberNotInAdminListIsForbidden() throws Exception {
        MockHttpSession session = logIn("plain_user");
        mockMvc.perform(get("/manage").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/profile").session(session))
                .andExpect(content().string(not(containsString("관리자</a>"))));
    }

    @Test
    void adminSeesDashboardAndHeaderLink() throws Exception {
        MockHttpSession session = logIn("boss_admin");
        mockMvc.perform(get("/manage").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("복지 데이터 수집")))
                .andExpect(content().string(containsString("공공데이터 API 호출")));
        mockMvc.perform(get("/profile").session(session))
                .andExpect(content().string(containsString("관리자</a>")));
    }

    @Test
    void collectButtonRunsBatchAndReportsResult() throws Exception {
        when(batchService.syncAll()).thenReturn(new BatchSyncResult(4818, 4818, 25, false,
                LocalDateTime.now()));
        MockHttpSession session = logIn("boss_admin");

        mockMvc.perform(post("/manage/batch").session(session).with(csrf()))
                .andExpect(redirectedUrl("/manage"))
                .andExpect(flash().attribute("batchMessage", "수집 완료: 4,818건 (API 호출 25회)"));
        verify(batchService).syncAll();
    }

    @Test
    void summaryResetClearsOneOrAllAndIsAdminOnly() throws Exception {
        benefitRepository.save(new WelfareBenefit("S1", "혜택1", null, "서울특별시", null, null, null, null, null,
                null, "20261101", "청년", "저소득", "주거", LocalDateTime.now()));
        detailRepository.save(new WelfareBenefitDetail("S1", "대상", "기준", "내용", "방법", null, null, "요약1"));
        detailRepository.save(new WelfareBenefitDetail("S2", "대상", "기준", "내용", "방법", null, null, "요약2"));
        detailRepository.save(new WelfareBenefitDetail("S3", "대상", "기준", "내용", "방법", null, null, "요약3"));

        mockMvc.perform(post("/manage/summaries/reset").session(logIn("plain_user")).with(csrf()))
                .andExpect(status().isForbidden());

        MockHttpSession admin = logIn("boss_admin");
        mockMvc.perform(post("/manage/summaries/reset").session(admin).with(csrf()).param("servId", "S1"))
                .andExpect(redirectedUrl("/manage"))
                .andExpect(flash().attribute("summaryMessage", "S1 요약 1건을 지웠어요."));
        mockMvc.perform(post("/manage/summaries/reset").session(admin).with(csrf()).param("servId", ""))
                .andExpect(flash().attribute("summaryMessage", "저장된 요약 2건을 모두 지웠어요."));
        entityManager.clear();
        assertThat(detailRepository.countByLlmSummaryIsNotNull()).isZero();

        mockMvc.perform(get("/manage").session(admin))
                .andExpect(content().string(containsString("코드표 라벨 대조")))
                .andExpect(content().string(containsString("저소득")));
    }

    @Test
    void pageViewsAreCountedButStaticFilesAreNot() throws Exception {
        long before = pageViewsToday();
        mockMvc.perform(get("/profile"));
        mockMvc.perform(get("/css/app.css"));
        mockMvc.perform(get("/favicon.ico"));
        assertThat(pageViewsToday() - before).isEqualTo(1);
    }

    private long pageViewsToday() {
        // 카운트는 별도 트랜잭션에서 커밋되므로, 테스트 트랜잭션이 들고 있는 예전 엔티티를 버리고 다시 읽는다.
        entityManager.clear();
        return metricRepository.findByMetricDateGreaterThanEqual(LocalDate.now(ZoneId.of("Asia/Seoul"))).stream()
                .filter(metric -> metric.getMetricKey().equals(DailyMetric.PAGE_VIEW))
                .mapToLong(DailyMetric::getMetricCount)
                .sum();
    }

    private MockHttpSession logIn(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin("/login").user(username).password(PASSWORD))
                .andReturn().getRequest().getSession(false);
    }
}
