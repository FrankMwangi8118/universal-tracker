package com.codify.universaltracker.field.service;

import com.codify.universaltracker.common.exception.BusinessRuleException;
import com.codify.universaltracker.common.exception.DuplicateResourceException;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.common.util.SlugUtil;
import com.codify.universaltracker.field.dto.*;
import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.entity.FieldOption;
import com.codify.universaltracker.field.enums.FieldType;
import com.codify.universaltracker.field.repository.FieldDefinitionRepository;
import com.codify.universaltracker.field.repository.FieldOptionRepository;
import com.codify.universaltracker.tracker.entity.Tracker;
import com.codify.universaltracker.tracker.service.TrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class FieldDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(FieldDefinitionService.class);

    private final FieldDefinitionRepository fieldRepo;
    private final FieldOptionRepository optionRepo;
    private final TrackerService trackerService;

    public FieldDefinitionService(FieldDefinitionRepository fieldRepo,
                                   FieldOptionRepository optionRepo,
                                   TrackerService trackerService) {
        this.fieldRepo = fieldRepo;
        this.optionRepo = optionRepo;
        this.trackerService = trackerService;
    }

    @Transactional
    public FieldDefinitionResponse createField(UUID trackerId, CreateFieldRequest request) {
        Tracker tracker = trackerService.findOwnedTracker(trackerId);
        validateTypeSpecificRules(request.fieldType(), request.options(), request.formulaExpression());

        String slug = SlugUtil.toUniqueSlug(request.name(),
                candidate -> fieldRepo.existsByTrackerIdAndSlug(trackerId, candidate));

        int nextOrder = fieldRepo.findMaxSortOrderByTrackerId(trackerId) + 1;

        FieldDefinition field = new FieldDefinition();
        field.setTrackerId(trackerId);
        field.setName(request.name());
        field.setSlug(slug);
        field.setFieldType(request.fieldType());
        applyDefaults(field, request.fieldType());
        applyCreateRequest(field, request, nextOrder);

        FieldDefinition saved = fieldRepo.save(field);
        log.info("Created field '{}' (type={}) on tracker {}", saved.getName(), saved.getFieldType(), trackerId);

        List<FieldOption> savedOptions = saveOptions(saved.getId(), request.options());
        return FieldDefinitionResponse.from(saved, savedOptions.stream().map(FieldOptionResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<FieldDefinitionListResponse> listFields(UUID trackerId) {
        trackerService.findOwnedTracker(trackerId);
        return fieldRepo.findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(trackerId)
                .stream()
                .map(f -> {
                    List<FieldOptionResponse> options = optionRepo
                            .findByFieldDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(f.getId())
                            .stream().map(FieldOptionResponse::from).toList();
                    return FieldDefinitionListResponse.from(f, options);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldDefinitionResponse getField(UUID fieldId) {
        FieldDefinition field = findField(fieldId);
        List<FieldOptionResponse> options = optionRepo
                .findByFieldDefinitionIdOrderBySortOrderAsc(fieldId)
                .stream().map(FieldOptionResponse::from).toList();
        return FieldDefinitionResponse.from(field, options);
    }

    @Transactional
    public FieldDefinitionResponse updateField(UUID fieldId, UpdateFieldRequest request) {
        FieldDefinition field = findField(fieldId);

        // Block field type change if data already exists
        if (fieldRepo.countValuesByFieldId(fieldId) > 0) {
            if (request.isActive() != null && !request.isActive()) {
                // Soft-deactivate is always allowed
            }
            // Any structural property changes are fine; only type change is blocked
        }

        if (request.name() != null && !request.name().equals(field.getName())) {
            String newSlug = SlugUtil.toUniqueSlug(request.name(),
                    candidate -> !candidate.equals(field.getSlug())
                            && fieldRepo.existsByTrackerIdAndSlug(field.getTrackerId(), candidate));
            field.setName(request.name());
            field.setSlug(newSlug);
        }
        if (request.isRequired() != null) field.setIsRequired(request.isRequired());
        if (request.isUnique() != null) field.setIsUnique(request.isUnique());
        if (request.isFilterable() != null) field.setIsFilterable(request.isFilterable());
        if (request.isSummable() != null) field.setIsSummable(request.isSummable());
        if (request.isPrimaryDisplay() != null) field.setIsPrimaryDisplay(request.isPrimaryDisplay());
        if (request.defaultValue() != null) field.setDefaultValue(request.defaultValue());
        if (request.displayFormat() != null) field.setDisplayFormat(request.displayFormat());
        if (request.placeholder() != null) field.setPlaceholder(request.placeholder());
        if (request.helpText() != null) field.setHelpText(request.helpText());
        if (request.currencyCode() != null) field.setCurrencyCode(request.currencyCode());
        if (request.minValue() != null) field.setMinValue(request.minValue());
        if (request.maxValue() != null) field.setMaxValue(request.maxValue());
        if (request.conditionalLogic() != null) field.setConditionalLogic(request.conditionalLogic());
        if (request.formulaExpression() != null) field.setFormulaExpression(request.formulaExpression());
        if (request.sortOrder() != null) field.setSortOrder(request.sortOrder());
        if (request.isActive() != null) field.setIsActive(request.isActive());

        FieldDefinition saved = fieldRepo.save(field);

        // Replace options if provided
        List<FieldOption> options;
        if (request.options() != null) {
            optionRepo.deleteAllByFieldDefinitionId(fieldId);
            options = saveOptions(fieldId, request.options());
        } else {
            options = optionRepo.findByFieldDefinitionIdOrderBySortOrderAsc(fieldId);
        }

        log.info("Updated field id={}", fieldId);
        return FieldDefinitionResponse.from(saved, options.stream().map(FieldOptionResponse::from).toList());
    }

    @Transactional
    public void deleteField(UUID fieldId) {
        FieldDefinition field = findField(fieldId);
        optionRepo.deleteAllByFieldDefinitionId(fieldId);
        fieldRepo.delete(field);
        log.info("Deleted field id={} (cascades to options, rules, field_values)", fieldId);
    }

    @Transactional
    public void reorderFields(UUID trackerId, List<UUID> orderedIds) {
        trackerService.findOwnedTracker(trackerId);
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            FieldDefinition field = fieldRepo.findById(id)
                    .filter(f -> f.getTrackerId().equals(trackerId))
                    .orElseThrow(() -> new ResourceNotFoundException("FieldDefinition", id));
            field.setSortOrder(i);
            fieldRepo.save(field);
        }
    }

    @Transactional
    public FieldDefinitionResponse duplicateField(UUID fieldId) {
        FieldDefinition original = findField(fieldId);
        List<FieldOption> originalOptions = optionRepo.findByFieldDefinitionIdOrderBySortOrderAsc(fieldId);

        String newName = original.getName() + " (copy)";
        String newSlug = SlugUtil.toUniqueSlug(newName,
                candidate -> fieldRepo.existsByTrackerIdAndSlug(original.getTrackerId(), candidate));

        int nextOrder = fieldRepo.findMaxSortOrderByTrackerId(original.getTrackerId()) + 1;

        FieldDefinition copy = new FieldDefinition();
        copy.setTrackerId(original.getTrackerId());
        copy.setName(newName);
        copy.setSlug(newSlug);
        copy.setFieldType(original.getFieldType());
        copy.setIsRequired(original.getIsRequired());
        copy.setIsUnique(false); // copies should not inherit unique constraint
        copy.setIsFilterable(original.getIsFilterable());
        copy.setIsSummable(original.getIsSummable());
        copy.setIsPrimaryDisplay(false);
        copy.setDefaultValue(original.getDefaultValue());
        copy.setDisplayFormat(original.getDisplayFormat());
        copy.setPlaceholder(original.getPlaceholder());
        copy.setHelpText(original.getHelpText());
        copy.setCurrencyCode(original.getCurrencyCode());
        copy.setMinValue(original.getMinValue());
        copy.setMaxValue(original.getMaxValue());
        copy.setConditionalLogic(original.getConditionalLogic());
        copy.setFormulaExpression(original.getFormulaExpression());
        copy.setSortOrder(nextOrder);

        FieldDefinition saved = fieldRepo.save(copy);

        List<FieldOption> copiedOptions = originalOptions.stream().map(o -> {
            FieldOption newOpt = new FieldOption();
            newOpt.setFieldDefinitionId(saved.getId());
            newOpt.setLabel(o.getLabel());
            newOpt.setValue(o.getValue());
            newOpt.setColor(o.getColor());
            newOpt.setIcon(o.getIcon());
            newOpt.setSortOrder(o.getSortOrder());
            newOpt.setIsDefault(o.getIsDefault());
            return optionRepo.save(newOpt);
        }).toList();

        log.info("Duplicated field id={} → new id={}", fieldId, saved.getId());
        return FieldDefinitionResponse.from(saved, copiedOptions.stream().map(FieldOptionResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // Option sub-operations

    @Transactional
    public FieldOptionResponse addOption(UUID fieldId, CreateFieldOptionRequest request) {
        FieldDefinition field = findField(fieldId);
        validateOptionType(field);

        if (optionRepo.existsByFieldDefinitionIdAndValue(fieldId, request.value())) {
            throw new DuplicateResourceException("FieldOption", "value", request.value());
        }

        FieldOption option = buildOption(fieldId, request, optionRepo.countByFieldDefinitionId(fieldId));
        FieldOption saved = optionRepo.save(option);
        return FieldOptionResponse.from(saved);
    }

    @Transactional
    public FieldOptionResponse updateOption(UUID fieldId, UUID optionId, UpdateFieldOptionRequest request) {
        findField(fieldId);
        FieldOption option = findOption(optionId, fieldId);

        if (request.label() != null) option.setLabel(request.label());
        if (request.value() != null) {
            if (!request.value().equals(option.getValue())
                    && optionRepo.existsByFieldDefinitionIdAndValue(fieldId, request.value())) {
                throw new DuplicateResourceException("FieldOption", "value", request.value());
            }
            option.setValue(request.value());
        }
        if (request.color() != null) option.setColor(request.color());
        if (request.icon() != null) option.setIcon(request.icon());
        if (request.sortOrder() != null) option.setSortOrder(request.sortOrder());
        if (request.isDefault() != null) option.setIsDefault(request.isDefault());
        if (request.isActive() != null) option.setIsActive(request.isActive());

        return FieldOptionResponse.from(optionRepo.save(option));
    }

    @Transactional
    public void deleteOption(UUID fieldId, UUID optionId) {
        findField(fieldId);
        FieldOption option = findOption(optionId, fieldId);
        optionRepo.delete(option);
    }

    @Transactional
    public void reorderOptions(UUID fieldId, List<UUID> orderedIds) {
        findField(fieldId);
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            FieldOption option = findOption(id, fieldId);
            option.setSortOrder(i);
            optionRepo.save(option);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers

    private FieldDefinition findField(UUID fieldId) {
        return fieldRepo.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldDefinition", fieldId));
    }

    private FieldOption findOption(UUID optionId, UUID fieldId) {
        FieldOption option = optionRepo.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldOption", optionId));
        if (!option.getFieldDefinitionId().equals(fieldId)) {
            throw new ResourceNotFoundException("FieldOption", optionId);
        }
        return option;
    }

    private void validateTypeSpecificRules(FieldType type, List<CreateFieldOptionRequest> options, String formula) {
        if ((type == FieldType.DROPDOWN || type == FieldType.MULTI_SELECT)
                && (options == null || options.isEmpty())) {
            throw new BusinessRuleException("DROPDOWN and MULTI_SELECT fields require at least one option");
        }
        if (type == FieldType.FORMULA && (formula == null || formula.isBlank())) {
            throw new BusinessRuleException("FORMULA fields require a formulaExpression");
        }
    }

    private void validateOptionType(FieldDefinition field) {
        if (field.getFieldType() != FieldType.DROPDOWN && field.getFieldType() != FieldType.MULTI_SELECT) {
            throw new BusinessRuleException("Options can only be added to DROPDOWN or MULTI_SELECT fields");
        }
    }

    private void applyDefaults(FieldDefinition field, FieldType type) {
        switch (type) {
            case RATING -> field.setMaxValue(BigDecimal.valueOf(5));
            case PROGRESS -> {
                field.setMinValue(BigDecimal.ZERO);
                field.setMaxValue(BigDecimal.valueOf(100));
            }
            default -> { /* no defaults */ }
        }
    }

    private void applyCreateRequest(FieldDefinition field, CreateFieldRequest request, int sortOrder) {
        field.setSortOrder(sortOrder);
        if (request.isRequired() != null) field.setIsRequired(request.isRequired());
        if (request.isUnique() != null) field.setIsUnique(request.isUnique());
        if (request.isFilterable() != null) field.setIsFilterable(request.isFilterable());
        if (request.isSummable() != null) field.setIsSummable(request.isSummable());
        if (request.isPrimaryDisplay() != null) field.setIsPrimaryDisplay(request.isPrimaryDisplay());
        if (request.defaultValue() != null) field.setDefaultValue(request.defaultValue());
        if (request.displayFormat() != null) field.setDisplayFormat(request.displayFormat());
        if (request.placeholder() != null) field.setPlaceholder(request.placeholder());
        if (request.helpText() != null) field.setHelpText(request.helpText());
        if (request.currencyCode() != null) field.setCurrencyCode(request.currencyCode());
        if (request.minValue() != null) field.setMinValue(request.minValue());
        if (request.maxValue() != null) field.setMaxValue(request.maxValue());
        if (request.conditionalLogic() != null) field.setConditionalLogic(request.conditionalLogic());
        if (request.formulaExpression() != null) field.setFormulaExpression(request.formulaExpression());
    }

    private List<FieldOption> saveOptions(UUID fieldId, List<CreateFieldOptionRequest> options) {
        if (options == null || options.isEmpty()) return List.of();
        return options.stream()
                .map(req -> optionRepo.save(buildOption(fieldId, req,
                        req.sortOrder() != null ? req.sortOrder() : options.indexOf(req))))
                .toList();
    }

    private FieldOption buildOption(UUID fieldId, CreateFieldOptionRequest request, int sortOrder) {
        FieldOption option = new FieldOption();
        option.setFieldDefinitionId(fieldId);
        option.setLabel(request.label());
        option.setValue(request.value());
        option.setColor(request.color());
        option.setIcon(request.icon());
        option.setSortOrder(request.sortOrder() != null ? request.sortOrder() : sortOrder);
        option.setIsDefault(request.isDefault() != null && request.isDefault());
        return option;
    }
}
