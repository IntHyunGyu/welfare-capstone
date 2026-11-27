package com.welfare.today.config;

import com.welfare.today.controller.BatchAdminController;
import com.welfare.today.dto.BatchSyncResult;
import com.welfare.today.service.WelfareBenefitBatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchAdminController.class)
@Import(AdminTokenInterceptor.class)
@TestPropertySource(properties = "welfare.admin.token=test-secret")
class AdminTokenInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WelfareBenefitBatchService batchService;

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(post("/admin/batch/welfare-list")).andExpect(status().isUnauthorized());
        verify(batchService, never()).syncAll();
    }

    @Test
    void wrongTokenIsRejected() throws Exception {
        mockMvc.perform(post("/admin/batch/welfare-list").header(AdminTokenInterceptor.HEADER, "nope"))
                .andExpect(status().isUnauthorized());
        verify(batchService, never()).syncAll();
    }

    @Test
    void correctTokenReachesController() throws Exception {
        when(batchService.syncAll()).thenReturn(new BatchSyncResult(0, 0, 0, false, LocalDateTime.now()));
        mockMvc.perform(post("/admin/batch/welfare-list").header(AdminTokenInterceptor.HEADER, "test-secret"))
                .andExpect(status().isOk());
        verify(batchService).syncAll();
    }

    @Test
    void blankTokenDisablesCheckForLocalDevelopment() {
        assertThat(new AdminTokenInterceptor("")
                .preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
                .isTrue();
    }
}
