package com.codify.universaltracker.view.service;

import com.codify.universaltracker.common.exception.BusinessRuleException;
import com.codify.universaltracker.view.dto.AggregateResponse;
import com.codify.universaltracker.view.dto.AggregateResponse.GroupEntry;
import com.codify.universaltracker.view.dto.AggregateResponse.SeriesEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AggregationService {

    private static final Set<String> VALID_FNS = Set.of("SUM", "AVG", "COUNT", "MIN", "MAX");
    private static final Set<String> VALID_BUCKETS = Set.of("day", "week", "month", "year");

    @PersistenceContext
    private EntityManager em;

    /**
     * Main aggregation entry point. Dispatches to scalar, grouped, or time-series.
     */
    @Transactional(readOnly = true)
    public AggregateResponse aggregate(
            UUID trackerId,
            String fieldSlug,
            String fn,
            String groupBySlug,
            String dateBucket,
            Instant from,
            Instant to) {

        String fnUpper = fn != null ? fn.toUpperCase() : "SUM";
        if (!VALID_FNS.contains(fnUpper)) {
            throw new BusinessRuleException("Invalid aggregation function: " + fn + ". Must be one of: SUM, AVG, COUNT, MIN, MAX");
        }

        if (dateBucket != null) {
            if (!VALID_BUCKETS.contains(dateBucket.toLowerCase())) {
                throw new BusinessRuleException("Invalid date bucket: " + dateBucket + ". Must be: day, week, month, year");
            }
            return timeSeries(trackerId, fieldSlug, fnUpper, dateBucket.toLowerCase(), from, to);
        }

        if (groupBySlug != null && !groupBySlug.isBlank()) {
            return grouped(trackerId, fieldSlug, fnUpper, groupBySlug, from, to);
        }

        return scalar(trackerId, fieldSlug, fnUpper, from, to);
    }

    // =========================================================================
    // Scalar: single aggregated value
    // =========================================================================

    private AggregateResponse scalar(UUID trackerId, String fieldSlug, String fn, Instant from, Instant to) {
        String valueColumn = "COUNT".equals(fn) ? "e.id" : "fv.value_number";
        String sql = """
                SELECT %s(%s)
                FROM entry e
                JOIN field_value fv ON fv.entry_id = e.id
                JOIN field_definition fd ON fd.id = fv.field_definition_id
                WHERE e.tracker_id = :trackerId
                  AND e.deleted_at IS NULL
                  AND fd.slug = :fieldSlug
                  AND (:from IS NULL OR e.entry_date >= :from)
                  AND (:to   IS NULL OR e.entry_date <= :to)
                """.formatted(fn, valueColumn);

        Object result = em.createNativeQuery(sql)
                .setParameter("trackerId", trackerId)
                .setParameter("fieldSlug", fieldSlug)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        BigDecimal total = result == null ? BigDecimal.ZERO : new BigDecimal(result.toString());
        return AggregateResponse.scalar(fn, fieldSlug, total);
    }

    // =========================================================================
    // Grouped: one row per distinct value of groupByField
    // =========================================================================

    @SuppressWarnings("unchecked")
    private AggregateResponse grouped(UUID trackerId, String fieldSlug, String fn,
                                       String groupBySlug, Instant from, Instant to) {
        String valueColumn = "COUNT".equals(fn) ? "e.id" : "fv.value_number";
        String sql = """
                SELECT
                    COALESCE(gfv.value_text, CAST(gfv.value_number AS TEXT), 'unknown') AS grp,
                    %s(%s) AS agg_value
                FROM entry e
                JOIN field_value fv  ON fv.entry_id  = e.id
                JOIN field_definition fd  ON fd.id   = fv.field_definition_id
                JOIN field_value gfv ON gfv.entry_id = e.id
                JOIN field_definition gfd ON gfd.id  = gfv.field_definition_id
                WHERE e.tracker_id = :trackerId
                  AND e.deleted_at IS NULL
                  AND fd.slug  = :fieldSlug
                  AND gfd.slug = :groupBySlug
                  AND (:from IS NULL OR e.entry_date >= :from)
                  AND (:to   IS NULL OR e.entry_date <= :to)
                GROUP BY grp
                ORDER BY agg_value DESC
                """.formatted(fn, valueColumn);

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("trackerId", trackerId)
                .setParameter("fieldSlug", fieldSlug)
                .setParameter("groupBySlug", groupBySlug)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<GroupEntry> groups = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "unknown";
            BigDecimal value = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            groups.add(new GroupEntry(label, value));
        }
        return AggregateResponse.grouped(fn, fieldSlug, groups);
    }

    // =========================================================================
    // Time series: one row per date bucket
    // =========================================================================

    @SuppressWarnings("unchecked")
    private AggregateResponse timeSeries(UUID trackerId, String fieldSlug, String fn,
                                          String bucket, Instant from, Instant to) {
        String valueColumn = "COUNT".equals(fn) ? "e.id" : "fv.value_number";

        // DATE_TRUNC format string per bucket
        String periodFormat = switch (bucket) {
            case "day"   -> "YYYY-MM-DD";
            case "week"  -> "IYYY-IW";
            case "month" -> "YYYY-MM";
            case "year"  -> "YYYY";
            default      -> "YYYY-MM";
        };

        String sql = """
                SELECT
                    TO_CHAR(DATE_TRUNC('%s', e.entry_date), '%s') AS period,
                    %s(%s) AS agg_value
                FROM entry e
                JOIN field_value fv ON fv.entry_id = e.id
                JOIN field_definition fd ON fd.id  = fv.field_definition_id
                WHERE e.tracker_id = :trackerId
                  AND e.deleted_at IS NULL
                  AND fd.slug = :fieldSlug
                  AND (:from IS NULL OR e.entry_date >= :from)
                  AND (:to   IS NULL OR e.entry_date <= :to)
                GROUP BY DATE_TRUNC('%s', e.entry_date)
                ORDER BY DATE_TRUNC('%s', e.entry_date) ASC
                """.formatted(bucket, periodFormat, fn, valueColumn, bucket, bucket);

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("trackerId", trackerId)
                .setParameter("fieldSlug", fieldSlug)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<SeriesEntry> series = new ArrayList<>();
        for (Object[] row : rows) {
            String period = row[0] != null ? row[0].toString() : "";
            BigDecimal value = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            series.add(new SeriesEntry(period, value));
        }
        return AggregateResponse.timeSeries(fn, fieldSlug, bucket, series);
    }

    // =========================================================================
    // Direct numeric query — used by DashboardService
    // =========================================================================

    @Transactional(readOnly = true)
    public BigDecimal scalarValue(UUID trackerId, String fieldSlug, String fn, Instant from, Instant to) {
        AggregateResponse result = scalar(trackerId, fieldSlug, fn.toUpperCase(), from, to);
        return result.total() != null ? result.total() : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long countEntries(UUID trackerId, Instant from, Instant to) {
        String sql = """
                SELECT COUNT(*) FROM entry e
                WHERE e.tracker_id = :trackerId
                  AND e.deleted_at IS NULL
                  AND (:from IS NULL OR e.entry_date >= :from)
                  AND (:to   IS NULL OR e.entry_date <= :to)
                """;
        Object result = em.createNativeQuery(sql)
                .setParameter("trackerId", trackerId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}
