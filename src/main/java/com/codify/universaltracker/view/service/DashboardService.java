package com.codify.universaltracker.view.service;

import com.codify.universaltracker.tracker.entity.Tracker;
import com.codify.universaltracker.tracker.service.TrackerService;
import com.codify.universaltracker.view.dto.DashboardResponse;
import com.codify.universaltracker.view.dto.DashboardResponse.CardResult;
import com.codify.universaltracker.view.dto.DashboardResponse.GoalResult;
import com.codify.universaltracker.view.dto.DashboardResponse.TrendResult;
import com.codify.universaltracker.view.dto.AggregateResponse.SeriesEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final TrackerService trackerService;
    private final AggregationService aggregationService;

    public DashboardService(TrackerService trackerService, AggregationService aggregationService) {
        this.trackerService = trackerService;
        this.aggregationService = aggregationService;
    }

    /**
     * Reads the tracker's summary_config and executes each card, trend, and goal aggregation.
     *
     * Expected summary_config shape:
     * {
     *   "cards": [
     *     {"label": "Total saved",   "field": "amount", "fn": "SUM"},
     *     {"label": "This month",    "field": "amount", "fn": "SUM", "period": "this_month"},
     *     {"label": "Total deposits","fn": "COUNT"}
     *   ],
     *   "trend": {"field": "amount", "fn": "SUM", "bucket": "month", "periods": 12},
     *   "goal":  {"field": "amount", "fn": "SUM", "target": 840000, "label": "Year-end goal"}
     * }
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID trackerId) {
        Tracker tracker = trackerService.findOwnedTracker(trackerId);
        Map<String, Object> config = tracker.getSummaryConfig();

        if (config == null || config.isEmpty()) {
            return new DashboardResponse(List.of(), null, null);
        }

        List<CardResult> cards = buildCards(trackerId, config, tracker.getDomainId().toString());
        TrendResult trend = buildTrend(trackerId, config);
        GoalResult goal = buildGoal(trackerId, config);

        return new DashboardResponse(cards, trend, goal);
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<CardResult> buildCards(UUID trackerId, Map<String, Object> config, String currencyCode) {
        Object cardsObj = config.get("cards");
        if (!(cardsObj instanceof List<?> cardList)) return List.of();

        List<CardResult> results = new ArrayList<>();
        for (Object item : cardList) {
            if (!(item instanceof Map<?, ?> card)) continue;

            String label = getString(card, "label", "Card");
            String fn    = getString(card, "fn", "SUM").toUpperCase();
            String field = getString(card, "field", null);
            String period = getString(card, "period", null);

            Instant[] range = resolvePeriod(period);
            BigDecimal value;

            if ("COUNT".equals(fn) && field == null) {
                value = BigDecimal.valueOf(aggregationService.countEntries(trackerId, range[0], range[1]));
            } else if (field != null) {
                value = aggregationService.scalarValue(trackerId, field, fn, range[0], range[1]);
            } else {
                value = BigDecimal.ZERO;
            }

            results.add(new CardResult(label, value, formatValue(value, fn)));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private TrendResult buildTrend(UUID trackerId, Map<String, Object> config) {
        Object trendObj = config.get("trend");
        if (!(trendObj instanceof Map<?, ?> trend)) return null;

        String field  = getString(trend, "field", null);
        String fn     = getString(trend, "fn", "SUM").toUpperCase();
        String bucket = getString(trend, "bucket", "month");
        int periods   = getInt(trend, "periods", 12);

        if (field == null) return null;

        Instant to   = Instant.now();
        Instant from = switch (bucket) {
            case "day"   -> to.minus(periods, ChronoUnit.DAYS);
            case "week"  -> to.minus(periods * 7L, ChronoUnit.DAYS);
            case "year"  -> to.minus(periods * 365L, ChronoUnit.DAYS);
            default      -> to.minus(periods * 30L, ChronoUnit.DAYS);
        };

        var agg = aggregationService.aggregate(trackerId, field, fn, null, bucket, from, to);
        List<SeriesEntry> series = agg.series() != null ? agg.series() : List.of();

        // Calculate trend direction from last 2 periods
        String direction = "flat";
        Double changePct = null;
        if (series.size() >= 2) {
            BigDecimal prev = series.get(series.size() - 2).value();
            BigDecimal curr = series.get(series.size() - 1).value();
            if (prev.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal change = curr.subtract(prev)
                        .divide(prev.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                changePct = change.doubleValue();
                direction = changePct > 0 ? "up" : changePct < 0 ? "down" : "flat";
            }
        }

        return new TrendResult(series, direction, changePct);
    }

    @SuppressWarnings("unchecked")
    private GoalResult buildGoal(UUID trackerId, Map<String, Object> config) {
        Object goalObj = config.get("goal");
        if (!(goalObj instanceof Map<?, ?> goal)) return null;

        String field  = getString(goal, "field", null);
        String fn     = getString(goal, "fn", "SUM").toUpperCase();
        String label  = getString(goal, "label", "Goal");

        Object targetObj = goal.get("target");
        if (field == null || targetObj == null) return null;

        BigDecimal target;
        try {
            target = new BigDecimal(targetObj.toString());
        } catch (NumberFormatException e) {
            return null;
        }

        BigDecimal current = aggregationService.scalarValue(trackerId, field, fn, null, null);
        double progressPct = target.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                current.divide(target, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

        return new GoalResult(target, current, Math.min(progressPct, 100.0), label);
    }

    // -------------------------------------------------------------------------

    private Instant[] resolvePeriod(String period) {
        if (period == null) return new Instant[]{null, null};
        Instant now = Instant.now();
        return switch (period) {
            case "today"      -> new Instant[]{now.truncatedTo(ChronoUnit.DAYS), null};
            case "this_week"  -> new Instant[]{now.minus(7, ChronoUnit.DAYS), null};
            case "this_month" -> new Instant[]{now.minus(30, ChronoUnit.DAYS), null};
            case "this_year"  -> new Instant[]{now.minus(365, ChronoUnit.DAYS), null};
            default           -> new Instant[]{null, null};
        };
    }

    private String formatValue(BigDecimal value, String fn) {
        if (value == null) return "0";
        if ("COUNT".equals(fn)) return String.valueOf(value.longValue());
        return String.format("%,.0f", value.doubleValue());
    }

    private String getString(Map<?, ?> map, String key, String defaultVal) {
        Object v = map.get(key);
        return v != null ? v.toString() : defaultVal;
    }

    private int getInt(Map<?, ?> map, String key, int defaultVal) {
        Object v = map.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
