// src/main/java/org/ewgf/models/converters/BattleTypeConverter.java
package org.ewgf.configuration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ewgf.models.BattleType;

@Converter(autoApply = false)
public class BattleTypeConverter implements AttributeConverter<BattleType,Integer> {

    @Override
    public Integer convertToDatabaseColumn(BattleType attribute) {
        return attribute == null ? null : attribute.getBattleCode();
    }

    @Override
    public BattleType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        for (BattleType t : BattleType.values()) {
            if (t.getBattleCode() == dbData) return t;
        }
        throw new IllegalArgumentException("Unknown battle_code " + dbData);
    }
}
