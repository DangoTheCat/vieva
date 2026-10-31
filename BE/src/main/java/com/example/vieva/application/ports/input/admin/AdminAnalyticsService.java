package com.example.vieva.application.ports.input.admin;

import com.example.vieva.application.ports.common.PageResult;

import java.util.UUID;

public interface AdminAnalyticsService {
    AdminStatsView getStats();
    default PageResult<AdminPerformanceItemView> getPerformance(int page, int size) {
        return getPerformance(null, null, null, null, page, size);
    }
    PageResult<AdminPerformanceItemView> getPerformance(UUID studentId, UUID subjectId, UUID examId, String status, int page, int size);
}
