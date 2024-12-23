package org.tekkenstats.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class EnumsMapper
{
    private Map<String, String> characterMap = new HashMap<>();
    private Map<String, String> stageMap = new HashMap<>();
    private Map<String, String> danMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("enums.json").getInputStream();

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
        } catch (Exception e)
        {
            throw new RuntimeException("Failed to load mappings:", e);
        }
    }

    public String getCharacterName(String characterId) {
        return characterMap.getOrDefault(characterId, "Undefined Character ID: " + characterId);
    }
    public String getStageName(String stageId) {
        return stageMap.getOrDefault(stageId, "Undefined stage name");
    }

    public String getDanName(String name) {
        return danMap.getOrDefault(name, "Undefined DAN name");
    }

}
