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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RegionService.class, WelfareMatchService.class})
class RegionTest {

    @Autowired
    private WelfareBenefitRepository repository;

    @Autowired
    private RegionService regionService;

    @Autowired
    private WelfareMatchService matchService;

    @BeforeEach
    void setUp() {
        repository.saveAll(List.of(
                benefit("SEOUL_WIDE", "서울특별시", null),
                benefit("SEOUL_BLANK_SGG", "서울특별시", ""),
                benefit("SONGPA", "서울특별시", "송파구"),
                benefit("SONGPA_2", "서울특별시", "송파구"),
                benefit("GANGNAM", "서울특별시", "강남구"),
                benefit("SEJONG", "세종특별자치시", null),
                benefit("BUSAN_JUNG", "부산광역시", "중구")
        ));
    }

    @Test
    void regionsComeFromStoredNamesSortedWithoutBlanksOrDuplicates() {
        Map<String, List<String>> regions = regionService.regions();

        assertThat(regions.keySet()).containsExactly("부산광역시", "서울특별시", "세종특별자치시");
        assertThat(regions.get("서울특별시")).containsExactly("강남구", "송파구");
        assertThat(regions.get("세종특별자치시")).isEmpty();
    }

    @Test
    void sggFilterIncludesSidoWideBenefits() {
        assertThat(ids(match("서울특별시", "송파구")))
                .containsExactlyInAnyOrder("SONGPA", "SONGPA_2", "SEOUL_WIDE", "SEOUL_BLANK_SGG");
    }

    @Test
    void sidoOnlyIncludesEverySggInThatSido() {
        assertThat(ids(match("서울특별시", null)))
                .containsExactlyInAnyOrder("SEOUL_WIDE", "SEOUL_BLANK_SGG", "SONGPA", "SONGPA_2", "GANGNAM");
    }

    private List<WelfareBenefit> match(String sido, String sgg) {
        return matchService.match(new WelfareProfile(List.of(), List.of(), List.of(), null, sido, sgg));
    }

    private static List<String> ids(List<WelfareBenefit> benefits) {
        return benefits.stream().map(WelfareBenefit::getServId).toList();
    }

    private static WelfareBenefit benefit(String id, String sido, String sgg) {
        return new WelfareBenefit(id, id + " 혜택", null, sido, sgg, null, null, null, null,
                null, "20261101", "청년", "저소득", "주거", LocalDateTime.now());
    }
}
