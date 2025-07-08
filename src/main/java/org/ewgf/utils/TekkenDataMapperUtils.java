package org.ewgf.utils;

import java.util.HashMap;
import java.util.Map;

public class TekkenDataMapperUtils {
    private static final Map<String, String> characterMap = new HashMap<>();
    private static final Map<String, String> stageMap = new HashMap<>();
    private static final Map<String, String> danMap = new HashMap<>();
    private static final Map<String, String> platformMap = new HashMap<>();

    static {
        characterMap.put("0", "Paul");
        characterMap.put("1", "Law");
        characterMap.put("2", "King");
        characterMap.put("3", "Yoshimitsu");
        characterMap.put("4", "Hwoarang");
        characterMap.put("5", "Xiaoyu");
        characterMap.put("6", "Jin");
        characterMap.put("7", "Bryan");
        characterMap.put("8", "Kazuya");
        characterMap.put("9", "Steve");
        characterMap.put("10", "Jack-8");
        characterMap.put("11", "Asuka");
        characterMap.put("12", "Devil Jin");
        characterMap.put("13", "Feng");
        characterMap.put("14", "Lili");
        characterMap.put("15", "Dragunov");
        characterMap.put("16", "Leo");
        characterMap.put("17", "Lars");
        characterMap.put("18", "Alisa");
        characterMap.put("19", "Claudio");
        characterMap.put("20", "Shaheen");
        characterMap.put("21", "Nina");
        characterMap.put("22", "Lee");
        characterMap.put("23", "Kuma");
        characterMap.put("24", "Panda");
        characterMap.put("28", "Zafina");
        characterMap.put("29", "Leroy");
        characterMap.put("32", "Jun");
        characterMap.put("33", "Reina");
        characterMap.put("34", "Azucena");
        characterMap.put("35", "Victor");
        characterMap.put("36", "Raven");
        characterMap.put("38", "Eddy");
        characterMap.put("39", "Lidia");
        characterMap.put("40", "Heihachi");
        characterMap.put("41", "Clive");
        characterMap.put("42", "Anna");
        characterMap.put("43", "Fahkumram");

        stageMap.put("100", "Arena");
        stageMap.put("101", "Arena Underground");
        stageMap.put("200", "Urban Square");
        stageMap.put("201", "Urban Square Evening");
        stageMap.put("300", "Yakushima");
        stageMap.put("400", "Coliseum of Fate");
        stageMap.put("500", "Rebel Hangar");
        stageMap.put("700", "Fallen Destiny");
        stageMap.put("900", "Descent into Subconscious");
        stageMap.put("1000", "Sanctum");
        stageMap.put("1100", "Into the Stratosphere");
        stageMap.put("1200", "Ortiz Farm");
        stageMap.put("1300", "Celebration On The Seine");
        stageMap.put("1400", "Secluded Training Ground");
        stageMap.put("1500", "Elegant Palace");
        stageMap.put("1600", "Midnight Siege");

        danMap.put("0", "Beginner");
        danMap.put("1", "1st Dan");
        danMap.put("2", "2nd Dan");
        danMap.put("3", "Fighter");
        danMap.put("4", "Strategist");
        danMap.put("5", "Combatant");
        danMap.put("6", "Brawler");
        danMap.put("7", "Ranger");
        danMap.put("8", "Cavalry");
        danMap.put("9", "Warrior");
        danMap.put("10", "Assailant");
        danMap.put("11", "Dominator");
        danMap.put("12", "Vanquisher");
        danMap.put("13", "Destroyer");
        danMap.put("14", "Eliminator");
        danMap.put("15", "Garyu");
        danMap.put("16", "Shinryu");
        danMap.put("17", "Tenryu");
        danMap.put("18", "Mighty Ruler");
        danMap.put("19", "Flame Ruler");
        danMap.put("20", "Battle Ruler");
        danMap.put("21", "Fujin");
        danMap.put("22", "Raijin");
        danMap.put("23", "Kishin");
        danMap.put("24", "Bushin");
        danMap.put("25", "Tekken King");
        danMap.put("26", "Tekken Emperor");
        danMap.put("27", "Tekken God");
        danMap.put("28", "Tekken God Supreme");
        danMap.put("29", "God of Destruction");
        danMap.put("30", "God of Destruction I");
        danMap.put("31", "God Of Destruction II");
        danMap.put("32", "God of Destruction III");
        danMap.put("33", "God of Destruction IV");
        danMap.put("34", "God of Destruction V");
        danMap.put("35", "God of Destruction VI");
        danMap.put("36", "God of Destruction VII");
        danMap.put ("37", "God of Destruction Infinity");
        danMap.put("100", "God of Destruction");
        danMap.put("101", "God of Destruction I");
        danMap.put("102", "God Of Destruction II");
        danMap.put("103", "God of Destruction III");
        danMap.put("104", "God of Destruction IV");
        danMap.put("105", "God of Destruction V");
        danMap.put("106", "God of Destruction VI");
        danMap.put("107", "God of Destruction VII");
        danMap.put("765", "God of Destruction Infinity");

        platformMap.put("3", "PC");
        platformMap.put("8", "PlayStation");
        platformMap.put("9", "XBOX");
    }

    public static String getCharacterName(String characterId) {
        return characterMap.getOrDefault(characterId, "Undefined Character ID: " + characterId);
    }

    public static String getStageName(String stageId) {
        return stageMap.getOrDefault(stageId, "Undefined stage name");
    }

    public static String getDanName(String name) {
        return danMap.getOrDefault(name, "Undefined DAN name");
    }

    public static String getPlatform(String platformId) {
        return platformMap.getOrDefault(platformId, "Undefined platform");
    }
}