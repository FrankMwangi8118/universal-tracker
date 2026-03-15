package com.codify.universaltracker.entry.service;

import com.codify.universaltracker.entry.dto.FieldValueDisplay;
import com.codify.universaltracker.entry.entity.FieldValue;
import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.entity.FieldOption;
import com.codify.universaltracker.field.enums.FieldType;
import com.codify.universaltracker.field.repository.FieldOptionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

@Component
public class DisplayFormatter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final FieldOptionRepository optionRepository;

    public DisplayFormatter(FieldOptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public FieldValueDisplay format(FieldDefinition field, FieldValue fv) {
        FieldType type = field.getFieldType();

        return switch (type) {
            case TEXT, URL, EMAIL, PHONE, IMAGE, COLOR, FORMULA ->
                    FieldValueDisplay.of(fv.getValueText(), fv.getValueText(), type);

            case NUMBER, RATING, PROGRESS, DURATION ->
                    formatNumber(fv.getValueNumber(), type);

            case CURRENCY ->
                    formatCurrency(fv.getValueNumber(), field.getCurrencyCode(), type);

            case CHECKBOX ->
                    FieldValueDisplay.of(fv.getValueBoolean(),
                            Boolean.TRUE.equals(fv.getValueBoolean()) ? "Yes" : "No", type);

            case DATE ->
                    formatDate(fv.getValueDate(), DATE_FMT, type);

            case DATETIME ->
                    formatDate(fv.getValueDate(), DATETIME_FMT, type);

            case TIME ->
                    formatDate(fv.getValueDate(), TIME_FMT, type);

            case DROPDOWN ->
                    formatDropdown(field.getId(), fv.getValueText(), type);

            case MULTI_SELECT ->
                    FieldValueDisplay.of(fv.getValueJson(), formatJson(fv.getValueJson()), type);
        };
    }

    // -------------------------------------------------------------------------

    private FieldValueDisplay formatNumber(BigDecimal value, FieldType type) {
        if (value == null) return FieldValueDisplay.of(null, "", type);
        String display = value.stripTrailingZeros().toPlainString();
        return FieldValueDisplay.of(value, display, type);
    }

    private FieldValueDisplay formatCurrency(BigDecimal value, String currencyCode, FieldType type) {
        if (value == null) return FieldValueDisplay.of(null, "", type);
        try {
            Currency currency = Currency.getInstance(currencyCode != null ? currencyCode : "USD");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);
            fmt.setCurrency(currency);
            fmt.setMinimumFractionDigits(0);
            fmt.setMaximumFractionDigits(2);
            // Replace default symbol with currency code for clarity: KES 14,000
            String formatted = currency.getCurrencyCode() + " "
                    + String.format("%,.0f", value.doubleValue());
            return FieldValueDisplay.of(value, formatted, type);
        } catch (Exception e) {
            return FieldValueDisplay.of(value, value.toPlainString(), type);
        }
    }

    private FieldValueDisplay formatDate(Instant value, DateTimeFormatter formatter, FieldType type) {
        if (value == null) return FieldValueDisplay.of(null, "", type);
        return FieldValueDisplay.of(value.toString(), formatter.format(value), type);
    }

    private FieldValueDisplay formatDropdown(java.util.UUID fieldId, String value, FieldType type) {
        if (value == null) return FieldValueDisplay.of(null, "", type);
        Optional<FieldOption> option = optionRepository.findByFieldDefinitionIdAndValue(fieldId, value);
        String label = option.map(FieldOption::getLabel).orElse(value);
        String color = option.map(FieldOption::getColor).orElse(null);
        return FieldValueDisplay.of(value, label, type, color);
    }

    private String formatJson(Object json) {
        if (json == null) return "";
        return json.toString();
    }
}
