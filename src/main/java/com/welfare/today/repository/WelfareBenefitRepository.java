package com.welfare.today.repository;

import com.welfare.today.entity.WelfareBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WelfareBenefitRepository extends JpaRepository<WelfareBenefit, String>,
        JpaSpecificationExecutor<WelfareBenefit> {

    @Query("select max(b.fetchedAt) from WelfareBenefit b")
    LocalDateTime findLastFetchedAt();

    @Query("select distinct b.ctpvNm as ctpvNm, b.sggNm as sggNm from WelfareBenefit b where b.ctpvNm is not null")
    List<RegionRow> findDistinctRegions();

    interface RegionRow {
        String getCtpvNm();

        String getSggNm();
    }
}
