package com.codify.universaltracker.view.dto;

import java.math.BigDecimal;
import java.util.List;

public record AggregateResponse(
        String aggregation,
        String field,
        BigDecimal total,
        String bucket,
        List<GroupEntry> groups,
        List<SeriesEntry> series
) {
    public record GroupEntry(String label, BigDecimal value) {}
    public record SeriesEntry(String period, BigDecimal value) {}

    public static AggregateResponse scalar(String fn, String field, BigDecimal total) {
        return new AggregateResponse(fn, field, total, null, null, null);
    }

    public static AggregateResponse grouped(String fn, String field, List<GroupEntry> groups) {
        return new AggregateResponse(fn, field, null, null, groups, null);
    }

    public static AggregateResponse timeSeries(String fn, String field, String bucket, List<SeriesEntry> series) {
        return new AggregateResponse(fn, field, null, bucket, null, series);
    }
}
