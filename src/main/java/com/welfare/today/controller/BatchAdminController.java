package com.welfare.today.controller;

import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.service.WelfareBenefitBatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 수동 실행용 엔드포인트. 배포 환경에서는 AdminTokenInterceptor가 X-Admin-Token 헤더를 검사한다
 * (외부에서 호출되면 하루 API 호출 quota가 낭비되므로).
 */
@RestController
@RequestMapping("/admin/batch")
public class BatchAdminController {

    private final WelfareBenefitBatchService batchService;

    public BatchAdminController(WelfareBenefitBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/welfare-list")
    public BatchSyncResult syncWelfareList() {
        return batchService.syncAll();
    }
}
