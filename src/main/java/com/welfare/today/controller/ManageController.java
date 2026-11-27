package com.welfare.today.controller;

import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.repository.WelfareBenefitDetailRepository;
import com.welfare.today.service.AdminDashboardService;
import com.welfare.today.service.LabelStatsService;
import com.welfare.today.service.WelfareBenefitBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

/**
 * 관리자 페이지. 접근 권한(ADMIN)은 SecurityConfig가 /manage/** 에 건다.
 * 예전에 curl + X-Admin-Token으로 부르던 관리 기능(배치, 요약 지우기, 라벨 대조)이 모두 여기로 옮겨왔다.
 */
@Controller
@RequestMapping("/manage")
public class ManageController {

    private static final Logger log = LoggerFactory.getLogger(ManageController.class);

    private final AdminDashboardService dashboardService;
    private final WelfareBenefitBatchService batchService;
    private final LabelStatsService labelStatsService;
    private final WelfareBenefitDetailRepository detailRepository;

    public ManageController(AdminDashboardService dashboardService, WelfareBenefitBatchService batchService,
                            LabelStatsService labelStatsService, WelfareBenefitDetailRepository detailRepository) {
        this.dashboardService = dashboardService;
        this.batchService = batchService;
        this.labelStatsService = labelStatsService;
        this.detailRepository = detailRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.dashboard());
        model.addAttribute("labelCounts", labelStatsService.labelCounts());
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

    /** 저장된 LLM 요약을 지워, 다음 상세 조회 때 현재 프롬프트/모델로 다시 만들게 한다. servId가 비면 전체. */
    @PostMapping("/summaries/reset")
    public String resetSummaries(@RequestParam(required = false) String servId, RedirectAttributes redirect) {
        boolean one = StringUtils.hasText(servId);
        int cleared = one ? detailRepository.clearSummary(servId.strip()) : detailRepository.clearAllSummaries();
        redirect.addFlashAttribute("summaryMessage", one
                ? String.format(Locale.KOREA, "%s 요약 %d건을 지웠어요.", servId.strip(), cleared)
                : String.format(Locale.KOREA, "저장된 요약 %,d건을 모두 지웠어요.", cleared));
        return "redirect:/manage";
    }
}
