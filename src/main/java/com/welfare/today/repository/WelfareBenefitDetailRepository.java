package com.welfare.today.repository;

import com.welfare.today.entity.WelfareBenefitDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface WelfareBenefitDetailRepository extends JpaRepository<WelfareBenefitDetail, String> {

    long countByLlmSummaryIsNotNull();

    @Transactional
    @Modifying
    @Query("update WelfareBenefitDetail d set d.llmSummary = null where d.llmSummary is not null")
    int clearAllSummaries();

    @Transactional
    @Modifying
    @Query("update WelfareBenefitDetail d set d.llmSummary = null where d.servId = :servId and d.llmSummary is not null")
    int clearSummary(String servId);
}
