package com.codify.universaltracker.view.dto;

import com.codify.universaltracker.view.entity.ViewConfig;
import com.codify.universaltracker.view.enums.ViewType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ViewConfigResponse(
        UUID id,
        UUID trackerId,
        String name,
        ViewType viewType,
        List<Map<String, Object>> columns,
        List<Map<String, Object>> sortRules,
        List<Map<String, Object>> filterRules,
        Map<String, Object> groupBy,
        List<Map<String, Object>> aggregations,
        Map<String, Object> chartConfig,
        Boolean isDefault,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static ViewConfigResponse from(ViewConfig v) {
        return new ViewConfigResponse(
                v.getId(), v.getTrackerId(), v.getName(), v.getViewType(),
                v.getColumns(), v.getSortRules(), v.getFilterRules(),
                v.getGroupBy(), v.getAggregations(), v.getChartConfig(),
                v.getIsDefault(), v.getSortOrder(),
                v.getCreatedAt(), v.getUpdatedAt()
        );
    }
}
