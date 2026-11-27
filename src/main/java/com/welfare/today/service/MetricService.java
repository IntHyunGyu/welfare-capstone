package com.welfare.today.service;

import com.welfare.today.entity.DailyMetric;
import com.welfare.today.repository.DailyMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 날짜별 카운터를 1 올린다. 배치처럼 바깥 트랜잭션이 롤백돼도 이미 쓴 API 호출은 quota에서 빠졌으므로
 * 카운트는 별도 트랜잭션(REQUIRES_NEW)으로 남긴다.
 */
@Service
public class MetricService {

    private static final Logger log = LoggerFactory.getLogger(MetricService.class);
    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final DailyMetricRepository repository;
    private final TransactionTemplate newTransaction;

    public MetricService(DailyMetricRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void increment(String key) {
        LocalDate today = LocalDate.now(SEOUL);
        try {
            newTransaction.executeWithoutResult(status -> {
                if (repository.increment(today, key) == 0) {
                    repository.saveAndFlush(new DailyMetric(today, key, 1));
                }
            });
        } catch (DataIntegrityViolationException e) {
            // 그날 첫 카운트를 두 요청이 동시에 만들려다 한쪽이 유니크 제약에 걸린 경우 - 이미 생긴 행을 올린다.
            newTransaction.executeWithoutResult(status -> repository.increment(today, key));
        } catch (RuntimeException e) {
            // 집계 실패가 사용자 요청이나 배치를 막으면 안 된다.
            log.warn("카운터 기록 실패 key={}: {}", key, e.getMessage());
        }
    }
}
