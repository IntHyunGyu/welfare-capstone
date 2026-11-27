package com.welfare.today.controller;

import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.service.AdminDashboardService;
import com.welfare.today.service.WelfareBenefitBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/** 관리자 페이지. 접근 권한(ADMIN)은 SecurityConfig가 /manage/** 에 건다. */
@Controller
@RequestMapping("/manage")
public class ManageController {

    private static final Logger log = LoggerFactory.getLogger(ManageController.class);

    private final AdminDashboardService dashboardService;
    private final WelfareBenefitBatchService batchService;

    public ManageController(AdminDashboardService dashboardService, WelfareBenefitBatchService batchService) {
        this.dashboardService = dashboardService;
        this.batchService = batchService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.dashboard());
        return "manage";
    }

    @PostMapping("/batch")
    public String runBatch(RedirectAttributes redirect) {
        try {
            BatchSyncResult result = batchService.syncAll();
            redirect.addFlashAttribute("batchMessage", String.format(Locale.KOREA, "수집 완료: %,d건 (API 호출 %d회)%s",
                    result.fetchedCount(), result.pageCount(),
                    result.truncatedByMaxPages() ? " — 최대 페이지 수에 걸려 일부만 받았어요" : ""));
        } catch (RuntimeException e) {
            log.error("관리자 페이지에서 실행한 목록조회 배치 실패", e);
            redirect.addFlashAttribute("batchError", "수집에 실패했어요: " + e.getMessage());
        }
        return "redirect:/manage";
    }
}
