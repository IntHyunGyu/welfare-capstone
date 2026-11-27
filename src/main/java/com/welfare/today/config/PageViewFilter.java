package com.welfare.today.config;

import com.welfare.today.entity.DailyMetric;
import com.welfare.today.service.MetricService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 사용자 화면 조회(GET) 수를 날짜별로 센다. 정적 파일(확장자 있는 경로)과 관리용 경로는 제외. */
@Component
public class PageViewFilter extends OncePerRequestFilter {

    private final MetricService metricService;

    public PageViewFilter(MetricService metricService) {
        this.metricService = metricService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isPageView(request)) {
            metricService.increment(DailyMetric.PAGE_VIEW);
        }
        chain.doFilter(request, response);
    }

    static boolean isPageView(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.contains(".")
                && !path.startsWith("/admin")
                && !path.startsWith("/manage")
                && !path.startsWith("/h2-console")
                && !path.startsWith("/error");
    }
}
