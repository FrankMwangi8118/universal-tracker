package com.codify.universaltracker.entry.service;

import com.codify.universaltracker.common.exception.BusinessRuleException;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.common.exception.ValidationException;
import com.codify.universaltracker.common.util.UserContext;
import com.codify.universaltracker.entry.dto.*;
import com.codify.universaltracker.entry.entity.Entry;
import com.codify.universaltracker.entry.entity.FieldValue;
import com.codify.universaltracker.entry.enums.EntryStatus;
import com.codify.universaltracker.entry.event.EntryCreatedEvent;
import com.codify.universaltracker.entry.event.EntryDeletedEvent;
import com.codify.universaltracker.entry.event.EntryUpdatedEvent;
import com.codify.universaltracker.entry.repository.EntryRepository;
import com.codify.universaltracker.entry.repository.FieldValueRepository;
import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.enums.FieldType;
import com.codify.universaltracker.field.repository.FieldDefinitionRepository;
import com.codify.universaltracker.tracker.entity.Tracker;
import com.codify.universaltracker.tracker.service.TrackerService;
import com.codify.universaltracker.validation.service.FieldValueValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class EntryService {

    private static final Logger log = LoggerFactory.getLogger(EntryService.class);

    private final EntryRepository entryRepository;
    private final FieldValueRepository fieldValueRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final TrackerService trackerService;
    private final FieldValueValidator fieldValueValidator;
    private final DisplayFormatter displayFormatter;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public EntryService(EntryRepository entryRepository,
                        FieldValueRepository fieldValueRepository,
                        FieldDefinitionRepository fieldDefinitionRepository,
                        TrackerService trackerService,
                        FieldValueValidator fieldValueValidator,
                        DisplayFormatter displayFormatter,
                        ApplicationEventPublisher eventPublisher) {
        this.entryRepository = entryRepository;
        this.fieldValueRepository = fieldValueRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.trackerService = trackerService;
        this.fieldValueValidator = fieldValueValidator;
        this.displayFormatter = displayFormatter;
        this.eventPublisher = eventPublisher;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Transactional
    public EntryResponse createEntry(UUID trackerId, CreateEntryRequest request) {
        UUID userId = UserContext.get();
        Tracker tracker = trackerService.findOwnedTracker(trackerId);

        List<FieldDefinition> allFields =
                fieldDefinitionRepository.findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(trackerId);

        Map<String, Object> submitted = request.fields() != null ? request.fields() : Map.of();

        // 1. Validate all submitted values — collect ALL errors
        Map<String, List<String>> allErrors = validateFields(allFields, submitted, null);
        if (!allErrors.isEmpty()) {
            throw new ValidationException(allErrors);
        }

        // 2. Persist Entry
        Entry entry = new Entry();
        entry.setTrackerId(trackerId);
        entry.setUserId(userId);
        entry.setEntryDate(request.entryDate() != null ? request.entryDate() : Instant.now());
        entry.setTags(request.tags());
        entry.setNotes(request.notes());
        entry.setMetadata(request.metadata());
        entry.setStatus(EntryStatus.ACTIVE);
        Entry saved = entryRepository.save(entry);

        // 3. Persist FieldValues
        List<FieldValue> fieldValues = buildAndSaveFieldValues(saved.getId(), allFields, submitted);

        log.info("Created entry id={} in tracker {}", saved.getId(), trackerId);
        eventPublisher.publishEvent(new EntryCreatedEvent(saved));

        return buildResponse(saved, allFields, fieldValues);
    }

    // =========================================================================
    // READ
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<EntryListResponse> listEntries(UUID trackerId, EntryFilterRequest filter) {
        trackerService.findOwnedTracker(trackerId);

        Pageable pageable = buildPageable(filter);

        Page<Entry> page;
        if (filter.getFieldFilters().isEmpty()) {
            page = findFilteredDynamic(trackerId, filter, pageable);
        } else {
            page = findFilteredWithFieldValues(trackerId, filter, pageable);
        }

        List<FieldDefinition> allFields =
                fieldDefinitionRepository.findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(trackerId);
        List<FieldDefinition> primaryFields = allFields.stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsPrimaryDisplay())).toList();

        return page.map(entry -> {
            List<FieldValue> values = fieldValueRepository.findByEntryId(entry.getId());
            Map<String, FieldValueDisplay> displayMap = buildPrimaryDisplayMap(primaryFields, values);
            return EntryListResponse.from(entry, displayMap);
        });
    }

    @Transactional(readOnly = true)
    public EntryResponse getEntry(UUID entryId) {
        Entry entry = findActiveEntry(entryId);
        ownershipCheck(entry);
        List<FieldDefinition> fields = fieldDefinitionRepository
                .findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(entry.getTrackerId());
        List<FieldValue> values = fieldValueRepository.findByEntryId(entryId);
        return buildResponse(entry, fields, values);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Transactional
    public EntryResponse updateEntry(UUID entryId, UpdateEntryRequest request) {
        Entry entry = findActiveEntry(entryId);
        ownershipCheck(entry);

        if (request.entryDate() != null) entry.setEntryDate(request.entryDate());
        if (request.status() != null) entry.setStatus(request.status());
        if (request.tags() != null) entry.setTags(request.tags());
        if (request.notes() != null) entry.setNotes(request.notes());
        if (request.metadata() != null) entry.setMetadata(request.metadata());

        List<FieldDefinition> allFields = fieldDefinitionRepository
                .findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(entry.getTrackerId());

        if (request.fields() != null && !request.fields().isEmpty()) {
            // Only validate and update submitted fields
            Map<String, FieldDefinition> fieldMap = buildSlugMap(allFields);
            Map<String, List<String>> errors = new LinkedHashMap<>();

            for (Map.Entry<String, Object> e : request.fields().entrySet()) {
                FieldDefinition field = fieldMap.get(e.getKey());
                if (field == null) continue;
                List<String> fieldErrors = fieldValueValidator.validate(field, e.getValue(), entryId);
                if (!fieldErrors.isEmpty()) errors.put(e.getKey(), fieldErrors);
            }
            if (!errors.isEmpty()) throw new ValidationException(errors);

            updateFieldValues(entry.getId(), allFields, request.fields());
        }

        Entry saved = entryRepository.save(entry);
        log.info("Updated entry id={}", entryId);
        eventPublisher.publishEvent(new EntryUpdatedEvent(saved));

        List<FieldValue> values = fieldValueRepository.findByEntryId(entryId);
        return buildResponse(saved, allFields, values);
    }

    // =========================================================================
    // DELETE / RESTORE
    // =========================================================================

    @Transactional
    public void deleteEntry(UUID entryId) {
        Entry entry = findActiveEntry(entryId);
        ownershipCheck(entry);
        entry.setDeletedAt(Instant.now());
        entryRepository.save(entry);
        log.info("Soft-deleted entry id={}", entryId);
        eventPublisher.publishEvent(new EntryDeletedEvent(entry));
    }

    @Transactional
    public EntryResponse restoreEntry(UUID entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", entryId));
        ownershipCheck(entry);
        if (entry.getDeletedAt() == null) {
            throw new BusinessRuleException("Entry is not deleted");
        }
        entry.setDeletedAt(null);
        Entry saved = entryRepository.save(entry);
        log.info("Restored entry id={}", entryId);
        List<FieldDefinition> fields = fieldDefinitionRepository
                .findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(entry.getTrackerId());
        List<FieldValue> values = fieldValueRepository.findByEntryId(entryId);
        return buildResponse(saved, fields, values);
    }

    @Transactional
    public void hardDeleteEntry(UUID entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", entryId));
        ownershipCheck(entry);
        fieldValueRepository.deleteAllByEntryId(entryId);
        entryRepository.delete(entry);
        log.info("Hard-deleted entry id={}", entryId);
    }

    // =========================================================================
    // BULK
    // =========================================================================

    @Transactional
    public List<EntryResponse> bulkCreateEntries(UUID trackerId, List<CreateEntryRequest> requests) {
        List<EntryResponse> results = new ArrayList<>();
        for (CreateEntryRequest req : requests) {
            results.add(createEntry(trackerId, req));
        }
        return results;
    }

    @Transactional
    public void bulkDeleteEntries(List<UUID> ids) {
        UUID userId = UserContext.get();
        int count = entryRepository.softDeleteByIds(ids, userId, Instant.now());
        log.info("Bulk soft-deleted {} entries for user {}", count, userId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Dynamic query — only adds WHERE clauses for non-null params.
     * Avoids the PostgreSQL "could not determine data type of parameter" error
     * that occurs when passing null for custom enum / timestamptz columns.
     */
    @SuppressWarnings("unchecked")
    private Page<Entry> findFilteredDynamic(UUID trackerId, EntryFilterRequest filter, Pageable pageable) {
        StringBuilder where = new StringBuilder(
                "WHERE e.tracker_id = :trackerId AND e.deleted_at IS NULL");

        if (filter.getStatus() != null) {
            where.append(" AND e.status = CAST(:status AS entry_status)");
        }
        if (filter.getFrom() != null) {
            where.append(" AND e.entry_date >= :from");
        }
        if (filter.getTo() != null) {
            where.append(" AND e.entry_date <= :to");
        }

        String dataSQL  = "SELECT * FROM entry e " + where + " ORDER BY e.entry_date DESC";
        String countSQL = "SELECT COUNT(*) FROM entry e " + where;

        var dataQuery  = entityManager.createNativeQuery(dataSQL,  Entry.class);
        var countQuery = entityManager.createNativeQuery(countSQL);

        dataQuery.setParameter("trackerId", trackerId);
        countQuery.setParameter("trackerId", trackerId);

        if (filter.getStatus() != null) {
            dataQuery.setParameter("status",  filter.getStatus().name().toLowerCase());
            countQuery.setParameter("status", filter.getStatus().name().toLowerCase());
        }
        if (filter.getFrom() != null) {
            dataQuery.setParameter("from",  filter.getFrom());
            countQuery.setParameter("from", filter.getFrom());
        }
        if (filter.getTo() != null) {
            dataQuery.setParameter("to",  filter.getTo());
            countQuery.setParameter("to", filter.getTo());
        }

        int pageSize   = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        dataQuery.setFirstResult(pageNumber * pageSize);
        dataQuery.setMaxResults(pageSize);

        List<Entry> content = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    private Entry findActiveEntry(UUID entryId) {
        return entryRepository.findByIdAndDeletedAtIsNull(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry", entryId));
    }

    private void ownershipCheck(Entry entry) {
        if (!entry.getUserId().equals(UserContext.get())) {
            throw new ResourceNotFoundException("Entry", entry.getId());
        }
    }

    private Map<String, List<String>> validateFields(
            List<FieldDefinition> allFields, Map<String, Object> submitted, UUID entryId) {

        Map<String, FieldDefinition> slugMap = buildSlugMap(allFields);
        Map<String, List<String>> errors = new LinkedHashMap<>();

        // Check required fields are present
        for (FieldDefinition field : allFields) {
            if (Boolean.TRUE.equals(field.getIsRequired()) && !submitted.containsKey(field.getSlug())) {
                errors.put(field.getSlug(), List.of("This field is required"));
            }
        }

        // Validate each submitted value
        for (Map.Entry<String, Object> e : submitted.entrySet()) {
            FieldDefinition field = slugMap.get(e.getKey());
            if (field == null) continue; // unknown slugs are silently ignored
            List<String> fieldErrors = fieldValueValidator.validate(field, e.getValue(), entryId);
            if (!fieldErrors.isEmpty()) errors.put(e.getKey(), fieldErrors);
        }

        return errors;
    }

    private List<FieldValue> buildAndSaveFieldValues(
            UUID entryId, List<FieldDefinition> allFields, Map<String, Object> submitted) {

        Map<String, FieldDefinition> slugMap = buildSlugMap(allFields);
        List<FieldValue> saved = new ArrayList<>();

        for (Map.Entry<String, Object> e : submitted.entrySet()) {
            FieldDefinition field = slugMap.get(e.getKey());
            if (field == null) continue;
            FieldValue fv = buildFieldValue(entryId, field, e.getValue());
            saved.add(fieldValueRepository.save(fv));
        }
        return saved;
    }

    private void updateFieldValues(
            UUID entryId, List<FieldDefinition> allFields, Map<String, Object> submitted) {

        Map<String, FieldDefinition> slugMap = buildSlugMap(allFields);

        for (Map.Entry<String, Object> e : submitted.entrySet()) {
            FieldDefinition field = slugMap.get(e.getKey());
            if (field == null) continue;

            FieldValue fv = fieldValueRepository
                    .findByEntryIdAndFieldDefinitionId(entryId, field.getId())
                    .orElse(new FieldValue());
            fv.setEntryId(entryId);
            fv.setFieldDefinitionId(field.getId());
            applyTypedValue(fv, field, e.getValue());
            fieldValueRepository.save(fv);
        }
    }

    private FieldValue buildFieldValue(UUID entryId, FieldDefinition field, Object rawValue) {
        FieldValue fv = new FieldValue();
        fv.setEntryId(entryId);
        fv.setFieldDefinitionId(field.getId());
        applyTypedValue(fv, field, rawValue);
        return fv;
    }

    private void applyTypedValue(FieldValue fv, FieldDefinition field, Object rawValue) {
        // Clear all typed columns first
        fv.setValueText(null);
        fv.setValueNumber(null);
        fv.setValueBoolean(null);
        fv.setValueDate(null);
        fv.setValueJson(null);

        if (rawValue == null) return;

        switch (field.getFieldType()) {
            case TEXT, DROPDOWN, URL, EMAIL, PHONE, IMAGE, COLOR ->
                    fv.setValueText(rawValue.toString());

            case NUMBER, CURRENCY, RATING, PROGRESS, DURATION -> {
                try {
                    fv.setValueNumber(new BigDecimal(rawValue.toString()));
                } catch (NumberFormatException e) {
                    fv.setValueText(rawValue.toString());
                }
            }
            case CHECKBOX ->
                    fv.setValueBoolean(parseBoolean(rawValue));

            case DATE, DATETIME, TIME -> {
                try {
                    if (rawValue instanceof Instant i) {
                        fv.setValueDate(i);
                    } else {
                        fv.setValueDate(Instant.parse(rawValue.toString()));
                    }
                } catch (Exception e) {
                    fv.setValueText(rawValue.toString());
                }
            }
            case MULTI_SELECT, FORMULA ->
                    fv.setValueJson(rawValue);
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        String s = value.toString().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private EntryResponse buildResponse(Entry entry, List<FieldDefinition> fields, List<FieldValue> values) {
        Map<UUID, FieldValue> valueByFieldId = new HashMap<>();
        for (FieldValue fv : values) {
            valueByFieldId.put(fv.getFieldDefinitionId(), fv);
        }

        Map<String, FieldValueDisplay> displayMap = new LinkedHashMap<>();
        for (FieldDefinition field : fields) {
            FieldValue fv = valueByFieldId.get(field.getId());
            if (fv != null) {
                displayMap.put(field.getSlug(), displayFormatter.format(field, fv));
            }
        }
        return EntryResponse.from(entry, displayMap);
    }

    private Map<String, FieldValueDisplay> buildPrimaryDisplayMap(
            List<FieldDefinition> primaryFields, List<FieldValue> values) {
        Map<UUID, FieldValue> valueByFieldId = new HashMap<>();
        for (FieldValue fv : values) valueByFieldId.put(fv.getFieldDefinitionId(), fv);

        Map<String, FieldValueDisplay> map = new LinkedHashMap<>();
        for (FieldDefinition field : primaryFields) {
            FieldValue fv = valueByFieldId.get(field.getId());
            if (fv != null) map.put(field.getSlug(), displayFormatter.format(field, fv));
        }
        return map;
    }

    private Map<String, FieldDefinition> buildSlugMap(List<FieldDefinition> fields) {
        Map<String, FieldDefinition> map = new HashMap<>();
        for (FieldDefinition f : fields) map.put(f.getSlug(), f);
        return map;
    }

    private Pageable buildPageable(EntryFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "entryDate");
        if (filter.getSort() != null && !filter.getSort().isBlank()) {
            String[] parts = filter.getSort().split(":");
            String field = parts[0];
            Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            // Map field slugs to Entry columns
            String column = switch (field) {
                case "created_at" -> "createdAt";
                case "updated_at" -> "updatedAt";
                default -> "entryDate";
            };
            sort = Sort.by(dir, column);
        }
        return PageRequest.of(filter.getPage(), filter.getSizeClamped(), sort);
    }

    // Native SQL dynamic filter — used when field-level filters are present
    @SuppressWarnings("unchecked")
    private Page<Entry> findFilteredWithFieldValues(
            UUID trackerId, EntryFilterRequest filter, Pageable pageable) {

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT e.* FROM entry e
                WHERE e.tracker_id = :trackerId
                  AND e.deleted_at IS NULL
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("trackerId", trackerId);

        if (filter.getStatus() != null) {
            sql.append(" AND e.status = CAST(:status AS entry_status)");
            params.put("status", filter.getStatus().name().toLowerCase());
        }
        if (filter.getFrom() != null) {
            sql.append(" AND e.entry_date >= :from");
            params.put("from", filter.getFrom());
        }
        if (filter.getTo() != null) {
            sql.append(" AND e.entry_date <= :to");
            params.put("to", filter.getTo());
        }

        // Field value filters
        int idx = 0;
        for (Map.Entry<String, String> fe : filter.getFieldFilters().entrySet()) {
            String key = fe.getKey();
            String val = fe.getValue();

            // Determine operator from suffix
            String slug = key;
            String operator = "=";
            String column = "value_text";

            if (key.endsWith("_gte")) { slug = key.substring(0, key.length() - 4); operator = ">="; column = "value_number"; }
            else if (key.endsWith("_lte")) { slug = key.substring(0, key.length() - 4); operator = "<="; column = "value_number"; }
            else if (key.endsWith("_gt"))  { slug = key.substring(0, key.length() - 3); operator = ">";  column = "value_number"; }
            else if (key.endsWith("_lt"))  { slug = key.substring(0, key.length() - 3); operator = "<";  column = "value_number"; }

            sql.append(String.format("""
                     AND EXISTS (
                       SELECT 1 FROM field_value fv%d
                       JOIN field_definition fd%d ON fd%d.id = fv%d.field_definition_id
                       WHERE fv%d.entry_id = e.id
                         AND fd%d.slug = :slug%d
                         AND fv%d.%s %s :val%d
                     )
                    """, idx, idx, idx, idx, idx, idx, idx, idx, column, operator, idx));

            params.put("slug" + idx, slug);
            params.put("val" + idx, "value_number".equals(column) ? new BigDecimal(val) : val);
            idx++;
        }

        sql.append(" ORDER BY e.entry_date DESC");

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), Entry.class);
        params.forEach(nativeQuery::setParameter);

        // Count query for pagination
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS cnt";
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);

        nativeQuery.setFirstResult((int) pageable.getOffset());
        nativeQuery.setMaxResults(pageable.getPageSize());

        List<Entry> results = nativeQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new org.springframework.data.domain.PageImpl<>(results, pageable, total);
    }
}
