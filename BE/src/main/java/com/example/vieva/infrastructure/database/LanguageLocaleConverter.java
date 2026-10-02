package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.LanguageLocale;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LanguageLocaleConverter implements AttributeConverter<LanguageLocale, String> {

    @Override
    public String convertToDatabaseColumn(LanguageLocale attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public LanguageLocale convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return LanguageLocale.fromCode(dbData);
    }
}
