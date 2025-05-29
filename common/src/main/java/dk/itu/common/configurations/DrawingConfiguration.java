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
    private final List<Style> styles = new ArrayList<>();

    public static DrawingConfiguration getInstance() {
        if (instance == null) {
            try (InputStream is = DrawingConfiguration.class.getClassLoader().getResourceAsStream("drawingConfig.yaml")) {
                instance = yamlMapper.readValue(is, DrawingConfiguration.class);
                instance.addCommonStyles();
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
        addStyle(Color.decode("#40739e80"), null);
        // Selected style => 2
        addStyle(Color.decode("#00FF0080"), null);
        // Route style => 3
        addStyle(Color.yellow, 6);
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
                    (style.rgba == null && color == null || Objects.requireNonNull(style.rgba).hashCode() == color.hashCode()) &&
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

    @JsonProperty
    private Specification specification;
    @JsonProperty
    private Map<String, Feature> features;

    public byte getStyle(Map<String, String> tags) {
        for (String key : tags.keySet()) {
            if (features.containsKey(key)) {
                // Found key
                String value = tags.get(key);
                Feature feature = features.get(key);
                if (feature.individuals != null) {
                    // Check individuals
                    if (feature.individuals.containsKey(value)) {
                        var style = feature.individuals.get(value);
                        return addStyle(specification.getColor(style.rgba), style.stroke);
                    }
                }
                if (feature.groupings != null) {
                    // Check groupings if individuals not found
                    for (Feature.Grouping grouping : feature.groupings) {
                        if (grouping.tags.contains(value)) {
                            return addStyle(specification.getColor(grouping.rgba), grouping.stroke);
                        }
                    }
                }
                if (feature.def != null) {
                    // Check default if groupings not found
                    return addStyle(specification.getColor(feature.def.rgba), feature.def.stroke);
                }
            }
        }
        // No color defined => shouldn't draw
        return -1;
    }

    public record Style(Color rgba, Integer stroke) {}

    private static class Specification {
        public final Map<String, Color> rgbaColors = new HashMap<>();
        @JsonCreator
        public Specification(@JsonProperty("rgbaColors") Map<String, String> hexColors) {
            for(String key : hexColors.keySet()) {
                rgbaColors.put(key, new Color(toARGB(javafx.scene.paint.Color.web(hexColors.get(key))), true)); // TODO: Check jfx color.web vs awt decode
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