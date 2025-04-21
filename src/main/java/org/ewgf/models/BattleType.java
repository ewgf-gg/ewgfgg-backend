package org.ewgf.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
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

    @JsonCreator
    public static BattleType fromCode(int code) {
        for (BattleType type : BattleType.values()) {
            if (type.getBattleCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown battle code: " + code);
    }
}
