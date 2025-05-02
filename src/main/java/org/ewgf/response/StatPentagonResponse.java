package org.ewgf.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class StatPentagonResponse {
    @JsonProperty("defense")
    private Integer defense;

    @JsonProperty("attack")
    private Integer attack;

    @JsonProperty("technique")
    private Integer technique;

    @JsonProperty("appeal")
    private Integer appeal;

    @JsonProperty("spirit")
    private Integer spirit;

    @JsonProperty("attackComponents")
    private Map<String, Integer> attackComponents;

    @JsonProperty("defenseComponents")
    private Map<String, Integer> defenseComponents;

    @JsonProperty("techniqueComponents")
    private Map<String, Integer> techniqueComponents;

    @JsonProperty("spiritComponents")
    private Map<String, Integer> spiritComponents;

    @JsonProperty("appealComponents")
    private Map<String, Integer> appealComponents;
}