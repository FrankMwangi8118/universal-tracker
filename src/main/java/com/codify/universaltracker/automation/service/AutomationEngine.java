package com.codify.universaltracker.automation.service;

import com.codify.universaltracker.automation.entity.Automation;
import com.codify.universaltracker.automation.entity.AutomationLog;
import com.codify.universaltracker.automation.enums.AutomationTrigger;
import com.codify.universaltracker.automation.repository.AutomationLogRepository;
import com.codify.universaltracker.automation.repository.AutomationRepository;
import com.codify.universaltracker.entry.entity.Entry;
import com.codify.universaltracker.entry.entity.FieldValue;
import com.codify.universaltracker.entry.enums.EntryStatus;
import com.codify.universaltracker.entry.event.EntryCreatedEvent;
import com.codify.universaltracker.entry.event.EntryDeletedEvent;
import com.codify.universaltracker.entry.event.EntryUpdatedEvent;
import com.codify.universaltracker.entry.repository.EntryRepository;
import com.codify.universaltracker.entry.repository.FieldValueRepository;
import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.repository.FieldDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Listens to entry lifecycle events and executes matching automations.
 * Runs asynchronously so it never blocks the HTTP request that triggered the event.
 */
@Service
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);

    private final AutomationRepository automationRepository;
    private final AutomationLogRepository logRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final FieldValueRepository fieldValueRepository;
    private final EntryRepository entryRepository;

    public AutomationEngine(AutomationRepository automationRepository,
                            AutomationLogRepository logRepository,
                            FieldDefinitionRepository fieldDefinitionRepository,
                            FieldValueRepository fieldValueRepository,
                            EntryRepository entryRepository) {
        this.automationRepository = automationRepository;
        this.logRepository = logRepository;
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.fieldValueRepository = fieldValueRepository;
        this.entryRepository = entryRepository;
    }

    // -------------------------------------------------------------------------
    // Event listeners

    @Async
    @EventListener
    public void onEntryCreated(EntryCreatedEvent event) {
        process(event.entry(), AutomationTrigger.ON_ENTRY_CREATED);
    }

    @Async
    @EventListener
    public void onEntryUpdated(EntryUpdatedEvent event) {
        process(event.entry(), AutomationTrigger.ON_ENTRY_UPDATED);
    }

    @Async
    @EventListener
    public void onEntryDeleted(EntryDeletedEvent event) {
        process(event.entry(), AutomationTrigger.ON_ENTRY_DELETED);
    }

    // -------------------------------------------------------------------------
    // Core processing

    private void process(Entry entry, AutomationTrigger trigger) {
        List<Automation> automations = automationRepository
                .findByTrackerIdAndIsActiveTrueAndTriggerEvent(entry.getTrackerId(), trigger);

        if (automations.isEmpty()) return;

        List<FieldValue> fieldValues = fieldValueRepository.findByEntryId(entry.getId());
        List<FieldDefinition> fields = fieldDefinitionRepository
                .findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(entry.getTrackerId());

        Map<String, FieldDefinition> fieldBySlug = new HashMap<>();
        for (FieldDefinition fd : fields) {
            fieldBySlug.put(fd.getSlug(), fd);
        }

        Map<UUID, FieldValue> valueByFieldId = new HashMap<>();
        for (FieldValue fv : fieldValues) {
            valueByFieldId.put(fv.getFieldDefinitionId(), fv);
        }

        for (Automation automation : automations) {
            try {
                if (conditionsMet(automation, fieldBySlug, valueByFieldId)) {
                    executeAction(automation, entry, fieldBySlug, valueByFieldId);
                }
            } catch (Exception ex) {
                log.error("Automation {} failed: {}", automation.getId(), ex.getMessage(), ex);
                writeLog(automation, entry, null, "failed", ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Condition evaluation

    /**
     * All conditions must pass (AND logic).
     * Condition shape: {"field": "slug", "operator": "eq|neq|gt|lt|gte|lte|contains|not_null", "value": ...}
     */
    private boolean conditionsMet(Automation automation,
                                  Map<String, FieldDefinition> fieldBySlug,
                                  Map<UUID, FieldValue> valueByFieldId) {
        List<Map<String, Object>> conditions = automation.getConditions();
        if (conditions == null || conditions.isEmpty()) return true;

        for (Map<String, Object> condition : conditions) {
            String slug = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object expected = condition.get("value");

            FieldDefinition fd = fieldBySlug.get(slug);
            if (fd == null) continue;

            FieldValue fv = valueByFieldId.get(fd.getId());
            if (!evaluate(fv, operator, expected)) return false;
        }
        return true;
    }

    private boolean evaluate(FieldValue fv, String operator, Object expected) {
        if ("not_null".equals(operator)) {
            return fv != null && hasAnyValue(fv);
        }
        if (fv == null) return false;

        // Prefer numeric comparison when value_number is set
        if (fv.getValueNumber() != null && expected != null) {
            BigDecimal actual = fv.getValueNumber();
            BigDecimal exp;
            try {
                exp = new BigDecimal(expected.toString());
            } catch (NumberFormatException e) {
                return false;
            }
            return switch (operator) {
                case "eq"  -> actual.compareTo(exp) == 0;
                case "neq" -> actual.compareTo(exp) != 0;
                case "gt"  -> actual.compareTo(exp) > 0;
                case "lt"  -> actual.compareTo(exp) < 0;
                case "gte" -> actual.compareTo(exp) >= 0;
                case "lte" -> actual.compareTo(exp) <= 0;
                default    -> false;
            };
        }

        // Text comparison
        if (fv.getValueText() != null) {
            String actual = fv.getValueText();
            String exp = expected == null ? "" : expected.toString();
            return switch (operator) {
                case "eq"       -> actual.equals(exp);
                case "neq"      -> !actual.equals(exp);
                case "contains" -> actual.contains(exp);
                default         -> false;
            };
        }

        // Boolean comparison
        if (fv.getValueBoolean() != null && expected != null) {
            boolean actual = Boolean.TRUE.equals(fv.getValueBoolean());
            boolean exp = Boolean.parseBoolean(expected.toString());
            return switch (operator) {
                case "eq"  -> actual == exp;
                case "neq" -> actual != exp;
                default    -> false;
            };
        }

        return false;
    }

    private boolean hasAnyValue(FieldValue fv) {
        return fv.getValueText() != null
                || fv.getValueNumber() != null
                || fv.getValueBoolean() != null
                || fv.getValueDate() != null
                || fv.getValueJson() != null;
    }

    // -------------------------------------------------------------------------
    // Action execution

    @Transactional
    protected void executeAction(Automation automation, Entry entry,
                                 Map<String, FieldDefinition> fieldBySlug,
                                 Map<UUID, FieldValue> valueByFieldId) {
        Map<String, Object> params = automation.getActionParams();
        if (params == null) params = Map.of();

        Map<String, Object> result = switch (automation.getActionType()) {
            case SET_FIELD_VALUE -> doSetFieldValue(entry, params, fieldBySlug, valueByFieldId);
            case UPDATE_STATUS   -> doUpdateStatus(entry, params);
            case CREATE_ENTRY    -> doCreateEntry(entry, params);
            case SEND_NOTIFICATION -> doSendNotification(entry, params, automation);
            case RUN_FORMULA     -> doRunFormula(entry, params, fieldBySlug, valueByFieldId);
            case WEBHOOK         -> Map.of("skipped", "webhook disabled");
        };

        // Update stats
        automation.setRunCount(automation.getRunCount() + 1);
        automation.setLastTriggered(Instant.now());
        automationRepository.save(automation);

        writeLog(automation, entry, result, "success", null);
    }

    // SET_FIELD_VALUE — params: {"field": "slug", "value": <raw>}
    private Map<String, Object> doSetFieldValue(Entry entry,
                                                Map<String, Object> params,
                                                Map<String, FieldDefinition> fieldBySlug,
                                                Map<UUID, FieldValue> valueByFieldId) {
        String slug = (String) params.get("field");
        Object value = params.get("value");

        FieldDefinition fd = fieldBySlug.get(slug);
        if (fd == null) return Map.of("error", "field not found: " + slug);

        FieldValue fv = valueByFieldId.computeIfAbsent(fd.getId(), k -> {
            FieldValue newFv = new FieldValue();
            newFv.setEntryId(entry.getId());
            newFv.setFieldDefinitionId(fd.getId());
            return newFv;
        });

        // Clear all columns then set the right one based on field type
        fv.setValueText(null);
        fv.setValueNumber(null);
        fv.setValueBoolean(null);
        fv.setValueDate(null);
        fv.setValueJson(null);

        switch (fd.getFieldType()) {
            case TEXT, DROPDOWN, URL, EMAIL, PHONE, IMAGE, COLOR ->
                    fv.setValueText(value == null ? null : value.toString());
            case NUMBER, CURRENCY, RATING, PROGRESS, DURATION -> {
                if (value != null) {
                    try { fv.setValueNumber(new BigDecimal(value.toString())); }
                    catch (NumberFormatException ignored) {}
                }
            }
            case CHECKBOX -> fv.setValueBoolean(value == null ? null : Boolean.parseBoolean(value.toString()));
            case DATE, DATETIME, TIME -> {
                if (value != null) {
                    try { fv.setValueDate(Instant.parse(value.toString())); }
                    catch (Exception ignored) {}
                }
            }
            default -> fv.setValueText(value == null ? null : value.toString());
        }

        fieldValueRepository.save(fv);
        return Map.of("field", slug, "set_to", value == null ? "null" : value.toString());
    }

    // UPDATE_STATUS — params: {"status": "active|archived|flagged|draft"}
    private Map<String, Object> doUpdateStatus(Entry entry, Map<String, Object> params) {
        String statusStr = (String) params.get("status");
        if (statusStr == null) return Map.of("error", "status param missing");

        EntryStatus newStatus;
        try {
            newStatus = EntryStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Map.of("error", "invalid status: " + statusStr);
        }

        entry.setStatus(newStatus);
        entryRepository.save(entry);
        return Map.of("status_set_to", newStatus.name().toLowerCase());
    }

    // CREATE_ENTRY — params: {"fields": {"slug": value, ...}}
    // Creates a minimal sibling entry in the same tracker (no validation run to avoid recursion)
    @SuppressWarnings("unchecked")
    private Map<String, Object> doCreateEntry(Entry source, Map<String, Object> params) {
        Map<String, Object> fieldParams = (Map<String, Object>) params.getOrDefault("fields", Map.of());

        Entry newEntry = new Entry();
        newEntry.setTrackerId(source.getTrackerId());
        newEntry.setUserId(source.getUserId());
        newEntry.setEntryDate(Instant.now());
        newEntry.setStatus(EntryStatus.ACTIVE);
        newEntry = entryRepository.save(newEntry);

        List<FieldDefinition> fields = fieldDefinitionRepository
                .findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(source.getTrackerId());
        Map<String, FieldDefinition> bySlug = new HashMap<>();
        for (FieldDefinition fd : fields) bySlug.put(fd.getSlug(), fd);

        List<FieldValue> valuesToSave = new ArrayList<>();
        for (Map.Entry<String, Object> fEntry : fieldParams.entrySet()) {
            FieldDefinition fd = bySlug.get(fEntry.getKey());
            if (fd == null) continue;
            FieldValue fv = new FieldValue();
            fv.setEntryId(newEntry.getId());
            fv.setFieldDefinitionId(fd.getId());
            fv.setValueText(fEntry.getValue() == null ? null : fEntry.getValue().toString());
            valuesToSave.add(fv);
        }
        fieldValueRepository.saveAll(valuesToSave);

        return Map.of("created_entry_id", newEntry.getId().toString());
    }

    // SEND_NOTIFICATION — logs the notification payload (no external delivery)
    private Map<String, Object> doSendNotification(Entry entry, Map<String, Object> params, Automation automation) {
        String message = (String) params.getOrDefault("message", "Automation triggered: " + automation.getName());
        String channel = (String) params.getOrDefault("channel", "in_app");
        log.info("[NOTIFICATION] channel={} entryId={} message={}", channel, entry.getId(), message);
        return Map.of("channel", channel, "message", message, "delivered", false, "note", "delivery not implemented");
    }

    // RUN_FORMULA — evaluates a simple arithmetic expression stored in action_params.expression
    // Supported: references to field slugs by {slug}, basic +/-/* /
    private Map<String, Object> doRunFormula(Entry entry, Map<String, Object> params,
                                             Map<String, FieldDefinition> fieldBySlug,
                                             Map<UUID, FieldValue> valueByFieldId) {
        String targetSlug = (String) params.get("target_field");
        String expression = (String) params.get("expression");
        if (expression == null || targetSlug == null) {
            return Map.of("error", "expression and target_field params required");
        }

        // Replace {slug} tokens with numeric field values
        String resolved = expression;
        for (Map.Entry<String, FieldDefinition> e : fieldBySlug.entrySet()) {
            FieldValue fv = valueByFieldId.get(e.getValue().getId());
            BigDecimal val = (fv != null && fv.getValueNumber() != null) ? fv.getValueNumber() : BigDecimal.ZERO;
            resolved = resolved.replace("{" + e.getKey() + "}", val.toPlainString());
        }

        BigDecimal result;
        try {
            result = evalSimpleExpression(resolved);
        } catch (Exception e) {
            return Map.of("error", "formula evaluation failed: " + e.getMessage());
        }

        FieldDefinition target = fieldBySlug.get(targetSlug);
        if (target == null) return Map.of("error", "target field not found: " + targetSlug);

        FieldValue fv = valueByFieldId.computeIfAbsent(target.getId(), k -> {
            FieldValue newFv = new FieldValue();
            newFv.setEntryId(entry.getId());
            newFv.setFieldDefinitionId(target.getId());
            return newFv;
        });
        fv.setValueNumber(result);
        fieldValueRepository.save(fv);

        return Map.of("expression", expression, "resolved", resolved, "result", result);
    }

    /**
     * Evaluates a simple left-to-right arithmetic expression (no precedence, no parentheses).
     * Good enough for basic formula automations like "a + b * c" where execution order matters less.
     */
    private BigDecimal evalSimpleExpression(String expr) {
        expr = expr.trim();
        // Tokenise on +, -, *, /
        List<String> tokens = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if ((c == '+' || c == '-' || c == '*' || c == '/') && !buf.isEmpty()) {
                tokens.add(buf.toString().trim());
                ops.add(c);
                buf = new StringBuilder();
            } else {
                buf.append(c);
            }
        }
        if (!buf.isEmpty()) tokens.add(buf.toString().trim());

        BigDecimal acc = new BigDecimal(tokens.get(0));
        for (int i = 0; i < ops.size(); i++) {
            BigDecimal next = new BigDecimal(tokens.get(i + 1));
            acc = switch (ops.get(i)) {
                case '+' -> acc.add(next);
                case '-' -> acc.subtract(next);
                case '*' -> acc.multiply(next);
                case '/' -> acc.divide(next, 4, java.math.RoundingMode.HALF_UP);
                default  -> acc;
            };
        }
        return acc;
    }

    // -------------------------------------------------------------------------
    // Logging

    private void writeLog(Automation automation, Entry entry,
                          Map<String, Object> actionResult, String status, String errorMessage) {
        AutomationLog logEntry = new AutomationLog();
        logEntry.setAutomationId(automation.getId());
        logEntry.setEntryId(entry.getId());
        logEntry.setTriggerData(Map.of(
                "trigger", automation.getTriggerEvent().name().toLowerCase(),
                "entry_id", entry.getId().toString(),
                "tracker_id", entry.getTrackerId().toString()
        ));
        logEntry.setActionResult(actionResult);
        logEntry.setStatus(status);
        logEntry.setErrorMessage(errorMessage);
        logRepository.save(logEntry);
    }
}
