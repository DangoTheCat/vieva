package com.example.vieva.adapters.controllers;

import com.example.vieva.adapters.presenters.PageResponse;
import com.example.vieva.application.ports.common.PageResult;
import com.example.vieva.application.ports.input.admin.AdminAnalyticsService;
import com.example.vieva.application.ports.input.admin.AdminPerformanceItemView;
import com.example.vieva.application.ports.input.admin.AdminStatsView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsView> getStats() {
        return ResponseEntity.ok(adminAnalyticsService.getStats());
    }

    @GetMapping("/performance")
    public ResponseEntity<PageResponse<AdminPerformanceItemView>> getPerformance(
            @RequestParam(name = "studentId", required = false) UUID studentId,
            @RequestParam(name = "subjectId", required = false) UUID subjectId,
            @RequestParam(name = "examId", required = false) UUID examId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        PageResult<AdminPerformanceItemView> result = adminAnalyticsService.getPerformance(
                studentId, subjectId, examId, status, page, size
        );
        return ResponseEntity.ok(PageResponse.from(result, Function.identity()));
    }
}
