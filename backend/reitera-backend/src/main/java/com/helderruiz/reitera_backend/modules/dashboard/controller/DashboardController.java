package com.helderruiz.reitera_backend.modules.dashboard.controller;

import com.helderruiz.reitera_backend.modules.dashboard.dto.DailyStudyCountDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.DashboardStatsDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.LastStudiedDeckDTO;
import com.helderruiz.reitera_backend.modules.dashboard.service.DashboardService;
import com.helderruiz.reitera_backend.modules.user.model.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes dashboard analytics endpoints.
 * All endpoints require a valid JWT (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Returns summary stats for the dashboard header badges:
     * study streak, total cards due today, and cards studied today.
     * <p>
     * GET /api/v1/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getStats(user));
    }

    /**
     * Returns the last {@code limit} decks the user has studied, with due/new/relearning counts.
     * The {@code limit} query parameter defaults to 5 (dashboard widget) but can be raised
     * by a future detail view without a second endpoint.
     *
     * <p>GET /api/v1/dashboard/last-studied
     */
    @GetMapping("/last-studied")
    public ResponseEntity<List<LastStudiedDeckDTO>> getLastStudied(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(dashboardService.getLastStudied(user, limit));
    }

    /**
     * Returns per-day card study counts for the past year (365 days ending today).
     * Only days with at least one review are included; missing days represent zero activity.
     *
     * <p>GET /api/v1/dashboard/heatmap
     */
    @GetMapping("/heatmap")
    public ResponseEntity<List<DailyStudyCountDTO>> getHeatmap(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getHeatmap(user));
    }
}
