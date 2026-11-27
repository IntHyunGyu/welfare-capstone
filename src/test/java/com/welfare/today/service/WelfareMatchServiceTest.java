package com.welfare.today.service;

import com.welfare.today.dto.WelfareProfile;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(WelfareMatchService.class)
class WelfareMatchServiceTest {

    @Autowired
    private WelfareBenefitRepository repository;

    @Autowired
    private WelfareMatchService matchService;

    @BeforeEach
    void setUp() {
        // 콤마구분 라벨은 실데이터 표기 그대로 (생애주기 임신·출산은 가운뎃점 양옆 공백)
        repository.saveAll(List.of(
                benefit("YOUTH_LOW", "청년", "저소득", "주거", "서울특별시", "20261101"),
                benefit("YOUTH_ONLY", "청년", "장애인", "일자리", "부산광역시", "20261020"),
                benefit("SENIOR_LOW", "노년,중장년", "저소득", "보호·돌봄", "서울특별시", "20260901"),
                benefit("PREGNANCY", "임신 · 출산,영유아", "다자녀", "임신·출산", "서울특별시", "20260801"),
                benefit("VETERAN", "노년", "보훈대상자", "생활지원", "경기도", "20260701")
        ));
    }

    @Test
    void emptyProfileReturnsEverythingNewestFirst() {
        assertThat(ids(match(List.of(), List.of(), List.of(), null)))
                .containsExactly("YOUTH_LOW", "YOUTH_ONLY", "SENIOR_LOW", "PREGNANCY", "VETERAN");
    }

    @Test
    void codesInSameCategoryAreOred() {
        assertThat(ids(match(List.of("004", "006"), List.of(), List.of(), null)))
                .containsExactlyInAnyOrder("YOUTH_LOW", "YOUTH_ONLY", "SENIOR_LOW", "VETERAN");
    }

    @Test
    void categoriesAreAnded() {
        assertThat(ids(match(List.of("004"), List.of("050"), List.of(), null)))
                .containsExactly("YOUTH_LOW");
    }

    @Test
    void sidoIsExactMatch() {
        assertThat(ids(match(List.of("004"), List.of(), List.of(), "서울특별시")))
                .containsExactly("YOUTH_LOW");
    }

    @Test
    void pregnancyLifeStageMatchesSpacedApiLabel() {
        assertThat(ids(match(List.of("007"), List.of(), List.of(), null)))
                .containsExactly("PREGNANCY");
    }

    @Test
    void veteranHouseholdMatches() {
        assertThat(ids(match(List.of(), List.of("030"), List.of(), null)))
                .containsExactly("VETERAN");
    }

    @Test
    void pregnancyThemeMatchesUnspacedApiLabel() {
        assertThat(ids(match(List.of(), List.of(), List.of("080"), null)))
                .containsExactly("PREGNANCY");
    }

    private List<WelfareBenefit> match(List<String> life, List<String> household, List<String> theme, String sido) {
        return matchService.match(new WelfareProfile(life, household, theme, null, sido, null));
    }

    private static List<String> ids(List<WelfareBenefit> benefits) {
        return benefits.stream().map(WelfareBenefit::getServId).toList();
    }

    private static WelfareBenefit benefit(String id, String life, String household, String theme,
                                          String sido, String lastModYmd) {
        return new WelfareBenefit(id, id + " 혜택", null, sido, null, null, null, null, null,
                null, lastModYmd, life, household, theme, LocalDateTime.now());
    }
}
