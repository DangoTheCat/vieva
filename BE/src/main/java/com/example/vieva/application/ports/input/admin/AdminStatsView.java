package com.example.vieva.application.ports.input.admin;

public record AdminStatsView(
        long totalUsers,
        long studentCount,
        long lecturerCount,
        long adminCount,
        long totalExams,
        long draftExams,
        long inProgressExams,
        long completedExams,
        long totalAttempts,
        long readyAttempts,
        long inProgressAttempts,
        long reconnectingAttempts,
        long finalizingAttempts,
        long completedAttempts,
        long interruptedAttempts
) {
    public AdminStatsView(
            long totalUsers,
            long studentCount,
            long lecturerCount,
            long adminCount,
            long totalExams,
            long totalAttempts,
            long completedAttempts
    ) {
        this(
                totalUsers,
                studentCount,
                lecturerCount,
                adminCount,
                totalExams,
                0L,
                0L,
                0L,
                totalAttempts,
                0L,
                0L,
                0L,
                0L,
                completedAttempts,
                0L
        );
    }
}
