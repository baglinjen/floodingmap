package dk.itu.common.configurations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.itu.util.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

import static dk.itu.util.DrawingUtils.toARGB;

public class DrawingConfiguration {
    @JsonIgnore
    private static DrawingConfiguration instance;
    @JsonIgnore
    private static final ObjectMapper yamlMapper = new YAMLMapper();
    @JsonIgnore
    private final Map<String, Byte> tagsToStyleId = new HashMap<>();
    @JsonIgnore
    private final Set<String> tagKeys = new HashSet<>();
    @JsonIgnore
    private final List<Style> styles = new ArrayList<>();

    public static DrawingConfiguration getInstance() {
        if (instance == null) {
            try (InputStream is = DrawingConfiguration.class.getClassLoader().getResourceAsStream("drawingConfig.yaml")) {
                instance = yamlMapper.readValue(is, DrawingConfiguration.class);
                instance.addCommonStyles();
                instance.tagKeys.add("highway"); // Highway should always be added => routing
                instance.tagKeys.add("type"); // Type should always be added => relations
                instance.createMapFromTagsToStyle();
            } catch (IOException e) {
                LoggerFactory.getLogger().error("Failed to load drawingConfig.yaml", e);
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private void addCommonStyles() {
        // Bounding box & above water style => 0
        addStyle(Color.BLACK, 1);
        // Below water style => 1
        addStyle(new Color(64, 115, 158, 127), null);
        // Selected style => 2
        addStyle(new Color(0, 255, 0, 127), null);
        // Route style => 3
        addStyle(Color.yellow, 6);
    }

    private void createMapFromTagsToStyle() {
        for (String featureTag : features.keySet()) {
            var feature = features.get(featureTag);
            tagKeys.add(featureTag);
            if (feature.individuals != null) {
                for (String individualTag : feature.individuals.keySet()) {
                    var individual = feature.individuals.get(individualTag);
                    tagsToStyleId.put(String.join("-", featureTag, individualTag), addStyle(specification.getColor(individual.rgba), individual.stroke));
                }
            }
            if (feature.groupings != null) {
                for (Feature.Grouping grouping : feature.groupings) {
                    for (String groupingTag : grouping.tags) {
                        String joinedTag = String.join("-", featureTag, groupingTag);
                        tagKeys.add(joinedTag);
                        tagsToStyleId.put(joinedTag, addStyle(specification.getColor(grouping.rgba), grouping.stroke));
                    }
                }
            }
            if (feature.def != null) {
                tagKeys.add(featureTag);
                tagsToStyleId.put(featureTag, addStyle(specification.getColor(feature.def.rgba), feature.def.stroke));
            }
        }
    }

    public Color getColor(byte styleId) {
        if (styleId < 0) return null;
        return styles.get(styleId).rgba;
    }
    public Integer getStroke(byte styleId) {
        if (styleId < 0) return null;
        return styles.get(styleId).stroke;
    }

    public byte addStyle(Color color, Integer stroke) {
        for (byte i = 0; i < this.styles.size(); i++) {
            var style = this.styles.get(i);
            // TODO: Improve null checks
            if (
                    (style.rgba == null && color == null || Objects.requireNonNull(style.rgba).hashCode() == Objects.requireNonNull(color).hashCode()) &&
                    (style.stroke == null && stroke == null || Objects.equals(style.stroke, stroke))
            ) {
                // Duplicate found
                return i;
            }
        }
        // No duplicates found => add entry
        styles.add(new Style(color, stroke));
        return (byte) (this.styles.size() - 1);
    }

    public Set<String> getTagKeys() {
        return tagKeys;
    }

    public byte getStyle(Map<String, String> tags) {
        byte styleId = -1;
        for (Map.Entry<String, String> entry :  tags.entrySet()) {
            styleId = tagsToStyleId.getOrDefault(String.join("-", entry.getKey(), entry.getValue()), styleId);
            if (styleId >= 0) {
                // Style found => stop
                break;
            } else {
                // Style not yet found => check defaults
                styleId = tagsToStyleId.getOrDefault(entry.getKey(), styleId);
                if (styleId >= 0) break;
            }
        }
        return styleId;
    }

    // JSON Mapping properties
    @JsonProperty
    private Specification specification;
    @JsonProperty
    private Map<String, Feature> features;

    public record Style(Color rgba, Integer stroke) {}

    private static class Specification {
        public final Map<String, Color> rgbaColors = new HashMap<>();
        @JsonCreator
        public Specification(@JsonProperty("rgbaColors") Map<String, String> hexColors) {
            for(String key : hexColors.keySet()) {
                rgbaColors.put(key, new Color(toARGB(javafx.scene.paint.Color.web(hexColors.get(key))), true));
            }
        }
        public Color getColor(String key) {
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