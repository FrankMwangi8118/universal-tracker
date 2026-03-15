package com.codify.universaltracker.entry.dto;

import com.codify.universaltracker.entry.enums.EntryStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EntryFilterRequest {

    private int page = 0;
    private int size = 25;
    private String sort;               // e.g. "amount:desc" or "date:asc"
    private EntryStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant to;

    private List<String> tags;

    // Populated by controller from ?filter[slug]=value, ?filter[slug_gte]=N etc.
    private Map<String, String> fieldFilters = new HashMap<>();

    public int getSizeClamped() {
        return Math.min(size, 100);
    }

    /**
     * Parses raw request params and extracts filter[*] entries into fieldFilters.
     * e.g. filter[where]=mmf  → key="where", value="mmf"
     *      filter[amount_gte]=10000 → key="amount_gte", value="10000"
     */
    public void parseFieldFilters(Map<String, String> allParams) {
        allParams.forEach((key, value) -> {
            if (key.startsWith("filter[") && key.endsWith("]")) {
                String fieldKey = key.substring(7, key.length() - 1);
                fieldFilters.put(fieldKey, value);
            }
        });
    }
}
