package com.welfare.today.controller;

import com.welfare.today.entity.WelfareBenefit;
import com.welfare.today.repository.WelfareBenefitRepository;
import com.welfare.today.service.WelfareBenefitDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BenefitDetailController {

    private final WelfareBenefitRepository benefitRepository;
    private final WelfareBenefitDetailService detailService;

    public BenefitDetailController(WelfareBenefitRepository benefitRepository,
                                   WelfareBenefitDetailService detailService) {
        this.benefitRepository = benefitRepository;
        this.detailService = detailService;
    }

    @GetMapping("/benefits/{servId}")
    public String detail(@PathVariable String servId, Model model) {
        WelfareBenefit benefit = benefitRepository.findById(servId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("benefit", benefit);
        model.addAttribute("detail", detailService.getOrFetch(benefit));
        return "benefit-detail";
    }
}
