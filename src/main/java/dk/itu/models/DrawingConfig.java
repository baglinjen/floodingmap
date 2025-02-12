package dk.itu.models;

import dk.itu.drawing.utils.ColorUtils;
import javafx.scene.paint.Color;
import kotlin.Pair;

import java.util.List;
import java.util.Map;

public record DrawingConfig(Specification specification, Map<String, Feature> features, List<String> infoToKeep) {
    public Pair<Integer, Integer> getColor(Map<String, String> tags) {
        for (String key : tags.keySet()) {
            if (key.equals("building")) {
                System.out.println("");
            }
            if (features.containsKey(key)) {
                // Found key
                String value = tags.get(key);
                Feature feature = features.get(key);
                if (feature.individuals != null) {
                    // Check individuals
                    if (feature.individuals.containsKey(value)) {
                        Integer level = feature.individuals.get(value).level;
                        return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(feature.individuals.get(value).rgb))), level == null ? specification.levels - 1 : level);
                    }
                }
                if (feature.groupings != null) {
                    for (Feature.Grouping grouping : feature.groupings) {
                        if (grouping.tags.contains(value)) {
                            Integer level = grouping.level;
                            return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(grouping.rgb))), level == null ? specification.levels - 1 : level);
                        }
                    }
                }
                if (feature.def != null) {
                    Integer level = feature.def.level;
                    return new Pair<>(ColorUtils.toARGB(Color.web(specification.getColorHex(feature.def.rgb))), level == null ? specification.levels - 1 : level);
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
            List<Grouping> groupings,
            Map<String, Individual> individuals,
            Individual def
    ) {
        private record Grouping(List<String> tags, String rgb, Integer level) {}
        private record Individual(String rgb, Integer level) {}
    }
}