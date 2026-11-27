package com.welfare.today.service;

import com.welfare.today.config.WelfareApiProperties;
import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.dto.WelfareItem;
import com.welfare.today.dto.WelfareListResponse;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({WelfareBenefitBatchService.class, WelfareBenefitBatchServiceTest.Props.class})
class WelfareBenefitBatchServiceTest {

    @TestConfiguration
    static class Props {
        @Bean
        WelfareApiProperties welfareApiProperties() {
            return new WelfareApiProperties("test-key", "http://localhost", 200, 50);
        }
    }

    @MockBean
    private WelfareApiService apiService;

    @Autowired
    private WelfareBenefitBatchService batchService;

    @Autowired
    private WelfareBenefitRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void firstFillIsNotNewButLaterArrivalsAreAndKeepTheirArrivalTime() {
        // 1) 빈 DB를 처음 채우는 수집 — 전부 처음 보지만 NEW로 치지 않는다
        apiReturns("A", "B");
        BatchSyncResult first = batchService.syncAll();
        assertThat(first.newCount()).isZero();
        assertThat(firstSeen("A")).isNull();
        assertThat(firstSeen("B")).isNull();

        // 2) 다음 수집 — 처음 보는 C만 NEW, 기존 A는 그대로
        apiReturns("A", "C");
        BatchSyncResult second = batchService.syncAll();
        assertThat(second.newCount()).isEqualTo(1);
        assertThat(firstSeen("A")).isNull();
        LocalDateTime cArrived = firstSeen("C");
        // DB 시각 정밀도(마이크로초)만큼은 차이가 날 수 있다
        assertThat(cArrived).isCloseTo(second.fetchedAt(), within(1, ChronoUnit.MILLIS));
        assertThat(repository.findById("C").orElseThrow().isNew()).isTrue();

        // 3) 그다음 수집 — C는 이미 있으니 처음 들어온 시각이 바뀌지 않는다
        apiReturns("A", "B", "C");
        BatchSyncResult third = batchService.syncAll();
        assertThat(third.newCount()).isZero();
        assertThat(firstSeen("C")).isEqualTo(cArrived);
    }

    private void apiReturns(String... ids) {
        List<WelfareItem> items = Arrays.stream(ids)
                .map(id -> new WelfareItem(id, id + " 혜택", null, "서울특별시", null, null, null, null, null,
                        null, "20261101", "청년", "저소득", "주거"))
                .toList();
        when(apiService.fetchList(anyInt()))
                .thenReturn(new WelfareListResponse(items.size(), 1, 200, 0, "OK", items));
    }

    private LocalDateTime firstSeen(String id) {
        entityManager.flush();
        entityManager.clear();
        return repository.findById(id).orElseThrow().getFirstSeenAt();
    }
}
