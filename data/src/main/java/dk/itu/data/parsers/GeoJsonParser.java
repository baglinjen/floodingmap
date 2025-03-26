package dk.itu.data.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.GeoJsonParserResult;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class GeoJsonParser {
    private static final Logger logger = LoggerFactory.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void parse(String fileName, GeoJsonParserResult geoJsonParserResult) {
        logger.info("Starting parsing file {}", fileName);
        try (InputStream is = CommonConfiguration.class.getClassLoader().getResourceAsStream("geojson/"+fileName)) {
            GeoJsonParserResult.GeoJsonFile geoJsonFile = objectMapper.readValue(is, GeoJsonParserResult.GeoJsonFile.class);
            geoJsonParserResult.addGeoJsonFile(geoJsonFile);
        } catch (IOException e) {
            logger.error("Failed to load file {}", fileName);
            throw new RuntimeException(e);
        }
    }


}
