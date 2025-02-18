package dk.itu.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.itu.utils.DrawingUtils.toARGB;

public record DrawingConfig(Specification specification, Map<String, Feature> features, List<String> infoToKeep) {
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
                        return new Style(specification.getColorInt(style.rgba), style.stroke);
                    }
                }
                if (feature.groupings != null) {
                    // Check groupings if individuals not found
                    for (Feature.Grouping grouping : feature.groupings) {
                        if (grouping.tags.contains(value)) {
                            return new Style(specification.getColorInt(grouping.rgba), grouping.stroke);
                        }
                    }
                }
                if (feature.def != null) {
                    // Check default if groupings not found
                    return new Style(specification.getColorInt(feature.def.rgba), feature.def.stroke);
                }
            }
        }
        // No color defined => shouldn't draw
        return null;
    }

    public record Style(Integer rgba, Integer stroke) {}

    private static class Specification {
        public final Map<String, Integer> rgbaColors = new HashMap<>();
        @JsonCreator
        public Specification(@JsonProperty("rgbaColors") Map<String, String> hexColors) {
            for(String key : hexColors.keySet()) {
                rgbaColors.put(key, toARGB(Color.web(hexColors.get(key))));
            }
        }
        public Integer getColorInt(String key) {
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