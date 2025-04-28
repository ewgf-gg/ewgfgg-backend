package org.ewgf.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class TekkenDataMapperUtils
{
    private static final Map<String, String> characterMap = new HashMap<>();
    private static final Map<String, String> stageMap = new HashMap<>();
    private static final Map<String, String> danMap = new HashMap<>();

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("tekken_enums.json").getInputStream();

            JsonNode root = mapper.readTree(inputStream);
            JsonNode characters = root.get("characters");
            JsonNode stages = root.get("stages");
            JsonNode dans = root.get("dan_names");

            characters.fields().forEachRemaining(entry ->
                    characterMap.put(entry.getKey(), entry.getValue().asText())
            );
            stages.fields().forEachRemaining(entry ->
                    stageMap.put(entry.getKey(), entry.getValue().asText())
            );
            dans.fields().forEachRemaining(entry ->
                    danMap.put(entry.getKey(), entry.getValue().asText())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mappings:", e);
        }
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
}
