package com.helderruiz.reitera_backend.modules.dashboard.dto;

/**
 * Summary statistics returned by GET /api/v1/dashboard/stats.
 *
 * @param streak        consecutive days with at least one review, ending today or yesterday
 * @param totalDueToday overdue progress-tracked cards + new cards across all user-owned decks
 * @param studiedToday  distinct cards reviewed today
 */
public record DashboardStatsDTO(int streak, long totalDueToday, long studiedToday) {
}
