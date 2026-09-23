package com.welfare.today.repository;

import com.welfare.today.entity.WelfareBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WelfareBenefitRepository extends JpaRepository<WelfareBenefit, String> {
}
