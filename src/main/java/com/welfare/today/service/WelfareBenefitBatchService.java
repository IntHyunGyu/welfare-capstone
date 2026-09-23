package com.welfare.today.service;

import com.welfare.today.config.WelfareApiProperties;
import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.dto.WelfareItem;
import com.welfare.today.dto.WelfareListResponse;
import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 목록조회 API를 페이지 단위로 반복 호출해 전체(또는 상한까지) 데이터를
 * 로컬 DB(WelfareBenefit)에 적재하는 배치. 스케줄러 없이 관리용 엔드포인트로
 * 수동 실행하는 것을 기본으로 한다 - 개발계정 일 1,000회 호출 제한 때문에
 * 자동/주기 실행은 위험하다.
 */
@Service
public class WelfareBenefitBatchService {

    private static final Logger log = LoggerFactory.getLogger(WelfareBenefitBatchService.class);

    private final WelfareApiService apiService;
    private final WelfareBenefitRepository repository;
    private final WelfareApiProperties properties;

    public WelfareBenefitBatchService(WelfareApiService apiService,
                                       WelfareBenefitRepository repository,
                                       WelfareApiProperties properties) {
        this.apiService = apiService;
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public BatchSyncResult syncAll() {
        LocalDateTime fetchedAt = LocalDateTime.now();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int fetchedCount = 0;
        boolean truncated = false;

        while ((pageNo - 1) * properties.pageSize() < totalCount) {
            if (pageNo > properties.maxPages()) {
                log.warn("welfare.api.max-pages({}) 도달 - 이번 실행에서는 여기까지만 수집한다. "
                        + "남은 데이터는 다음 실행에서 이어서 받으려면 목록조회 정렬/페이지 파라미터 보정이 필요하다.",
                        properties.maxPages());
                truncated = true;
                break;
            }

            WelfareListResponse response = apiService.fetchList(pageNo);
            if (response == null || response.servList() == null) {
                log.error("welfare list API 응답이 비어있거나 예상한 스키마가 아니다. pageNo={}", pageNo);
                break;
            }

            totalCount = response.totalCount();
            List<WelfareItem> items = response.servList();
            log.info("welfare list 수집 중: pageNo={}, 이번 페이지 건수={}, totalCount={}",
                    pageNo, items.size(), totalCount);

            if (items.isEmpty()) {
                break;
            }

            List<WelfareBenefit> entities = items.stream()
                    .map(item -> toEntity(item, fetchedAt))
                    .toList();
            repository.saveAll(entities);
            fetchedCount += entities.size();

            pageNo++;
        }

        log.info("welfare list 배치 수집 완료: fetchedCount={}, totalCount={}, truncated={}",
                fetchedCount, totalCount, truncated);
        return new BatchSyncResult(fetchedCount, totalCount, pageNo - 1, truncated, fetchedAt);
    }

    private WelfareBenefit toEntity(WelfareItem item, LocalDateTime fetchedAt) {
        return new WelfareBenefit(
                item.servId(),
                item.servNm(),
                item.servDgst(),
                item.ctpvNm(),
                item.sggNm(),
                item.bizChrDeptNm(),
                item.aplyMtdNm(),
                item.sprtCycNm(),
                item.srvPvsnNm(),
                item.servDtlLink(),
                item.lastModYmd(),
                item.lifeNmArray(),
                item.trgterIndvdlNmArray(),
                item.intrsThemaNmArray(),
                fetchedAt
        );
    }
}
