package dk.itu.common.configurations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.itu.util.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.itu.util.DrawingUtils.toARGB;

public class DrawingConfiguration {
    @JsonIgnore
    private static DrawingConfiguration instance;
    @JsonIgnore
    private static final ObjectMapper yamlMapper = new YAMLMapper();

    public static DrawingConfiguration getInstance() {
        if (instance == null) {
            try (InputStream is = DrawingConfiguration.class.getClassLoader().getResourceAsStream("drawingConfig.yaml")) {
                instance = yamlMapper.readValue(is, DrawingConfiguration.class);
            } catch (IOException e) {
                LoggerFactory.getLogger().error("Failed to load drawingConfig.yaml", e);
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    @JsonProperty
    private Specification specification;
    @JsonProperty
    private Map<String, Feature> features;
    @JsonProperty
    private List<String> infoToKeep;

    public Style getStyle(Map<String, String> tags) {
        for (String key : tags.keySet()) {
            if (features.containsKey(key)) {
                // Found key
                String value = tags.get(key);
                Feature feature = features.get(key);
                if (feature.individuals != null) {
                    // Check individuals
                    if (feature.individuals.containsKey(value)) {
                        var style = feature.individuals.get(value);
                        return new Style(specification.getColor(style.rgba), style.stroke);
                    }
                }
                if (feature.groupings != null) {
                    // Check groupings if individuals not found
                    for (Feature.Grouping grouping : feature.groupings) {
                        if (grouping.tags.contains(value)) {
                            return new Style(specification.getColor(grouping.rgba), grouping.stroke);
                        }
                    }
                }
                if (feature.def != null) {
                    // Check default if groupings not found
                    return new Style(specification.getColor(feature.def.rgba), feature.def.stroke);
                }
            }
        }
        // No color defined => shouldn't draw
        return null;
    }

    public record Style(java.awt.Color rgba, Integer stroke) {}

    private static class Specification {
        public final Map<String, java.awt.Color> rgbaColors = new HashMap<>();
        @JsonCreator
        public Specification(@JsonProperty("rgbaColors") Map<String, String> hexColors) {
            for(String key : hexColors.keySet()) {
                rgbaColors.put(key, new java.awt.Color(toARGB(javafx.scene.paint.Color.web(hexColors.get(key))), true));
            }
        }
        public java.awt.Color getColor(String key) {
            return rgbaColors.get(key);
        }
    }
    private record Feature(
            List<Grouping> groupings,
            Map<String, Individual> individuals,
            Individual def
    ) {
        private record Grouping(List<String> tags, String rgba, Integer stroke) {}
        private record Individual(String rgba, Integer stroke) {}
    }
}