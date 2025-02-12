package dk.itu.models;

import dk.itu.drawing.utils.ColorUtils;
import javafx.scene.paint.Color;
import kotlin.Pair;

import java.util.List;
import java.util.Map;

public record DrawingConfig(Specification specification, Map<String, Feature> features, List<String> infoToKeep) {
    public Pair<Integer, Integer> getColor(Map<String, String> tags) {
        for (String key : tags.keySet()) {
            if (features.containsKey(key)) {
                // Found key
                String value = tags.get(key);
                Feature feature = features.get(key);
                if (feature.individuals != null) {
                    // Check individuals
                    if (feature.individuals.containsKey(value)) {
                        return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(feature.individuals.get(value).rgb))), feature.level);
                    }
                }
                if (feature.groupings != null) {
                    for (Feature.Grouping grouping : feature.groupings) {
                        if (grouping.tags.contains(value)) {
                            return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(grouping.rgb))), feature.level);
                        }
                    }
                }
                if (feature.def != null) {
                    return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(feature.def.rgb))), feature.level);
                }
            }
        }
        return null;
    }

    public int getLevelsCount() {
        return specification.levels;
    }

    private record Specification(int levels, Map<String, String> rgbColors) {
        public String getColorHex(String key) {
            return rgbColors.get(key);
        }
    }
    private record Feature(
            int level,
            List<Grouping> groupings,
            Map<String, ColorSpecification> individuals,
            ColorSpecification def
    ) {
        private record Grouping(List<String> tags, String rgb) {}
        private record ColorSpecification(String rgb) {}
    }
}