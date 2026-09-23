package com.welfare.today.repository;

import com.welfare.today.entity.WelfareBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WelfareBenefitRepository extends JpaRepository<WelfareBenefit, String>,
        JpaSpecificationExecutor<WelfareBenefit> {
}
