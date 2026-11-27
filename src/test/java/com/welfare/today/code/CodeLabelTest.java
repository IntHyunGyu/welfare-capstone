package com.welfare.today.code;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 매칭은 코드표 enum 라벨로 DB의 콤마구분 문자열을 LIKE 검색하므로, 라벨이 API 표기와 한 글자라도 다르면
 * 그 항목만 조용히 0건이 된다. 아래 기대값은 2026-11-27 배포 DB(실데이터 4,818건)에서 집계한 표기다
 * (지금은 관리자 페이지의 "코드표 라벨 대조"에서 확인).
 */
class CodeLabelTest {

    private static final Set<String> API_LIFE_LABELS =
            Set.of("영유아", "아동", "청소년", "청년", "중장년", "노년", "임신 · 출산");

    private static final Set<String> API_HOUSEHOLD_LABELS =
            Set.of("다문화·탈북민", "다자녀", "보훈대상자", "장애인", "저소득", "한부모·조손");

    // 정신건강/문화·여가/법률은 실데이터에 없어 여기엔 없지만 enum에는 남겨 둔다 (API 코드표에는 존재).
    private static final Set<String> API_THEME_LABELS = Set.of(
            "신체건강", "생활지원", "주거", "일자리", "안전·위기", "임신·출산",
            "보육", "교육", "입양·위탁", "보호·돌봄", "서민금융");

    @Test
    void lifeStageLabelsMatchApiExactly() {
        assertThat(labels(LifeStage.values(), LifeStage::getLabel)).isEqualTo(API_LIFE_LABELS);
    }

    @Test
    void householdLabelsMatchApiExactly() {
        assertThat(labels(HouseholdSituation.values(), HouseholdSituation::getLabel)).isEqualTo(API_HOUSEHOLD_LABELS);
    }

    @Test
    void everyApiThemeLabelIsSelectable() {
        assertThat(labels(InterestTheme.values(), InterestTheme::getLabel)).containsAll(API_THEME_LABELS);
    }

    @Test
    void labelOfCodeResolvesKnownCodesAndRejectsUnknown() {
        assertThat(LifeStage.labelOfCode("007")).isEqualTo("임신 · 출산");
        assertThat(HouseholdSituation.labelOfCode("030")).isEqualTo("보훈대상자");
        assertThat(InterestTheme.labelOfCode("080")).isEqualTo("임신·출산");
        assertThat(LifeStage.labelOfCode("999")).isNull();
    }

    private static <E> Set<String> labels(E[] values, java.util.function.Function<E, String> label) {
        return Arrays.stream(values).map(label).collect(Collectors.toSet());
    }
}
