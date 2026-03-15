package com.codify.universaltracker.view.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        List<CardResult> cards,
        TrendResult trend,
        GoalResult goal
) {
    public record CardResult(String label, BigDecimal value, String formatted) {}

    public record TrendResult(
            List<AggregateResponse.SeriesEntry> series,
            String direction,     // "up", "down", "flat"
            Double changePct
    ) {}

    public record GoalResult(
            BigDecimal target,
            BigDecimal current,
            Double progressPct,
            String label
    ) {}
}
