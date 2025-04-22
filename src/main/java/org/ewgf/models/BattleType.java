package org.ewgf.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
public enum BattleType {
    QUICK_BATTLE(1),
    RANKED_BATTLE(2),
    GROUP_BATTLE(3),
    PLAYER_BATTLE(4);

    private int battleCode;

    private BattleType(int battleCode) {this.battleCode = battleCode;}

    public static Integer getCodeByName(String name) {
        try {
            return BattleType.valueOf(name).getBattleCode();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonValue
    public int toValue() {
        return battleCode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BattleType fromValue(int code) {
        for (BattleType t : values()) {
            if (t.battleCode == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown BattleType code: " + code);
    }
}
