package dk.itu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dk.itu.models.DrawingConfig;

import java.io.IOException;
import java.io.InputStream;

public class DrawingConfigParser {
    private static final ObjectMapper yamlMapper = new YAMLMapper();
    public static DrawingConfig parse() {
        try (InputStream is = JavaFxApp.class.getClassLoader().getResourceAsStream("drawingConfig.yaml")) {
            return yamlMapper.readValue(is, DrawingConfig.class);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Failed to parse yaml config file");
        }
    }
}