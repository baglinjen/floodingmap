package dk.itu.data.services;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;
import dk.itu.data.dto.GeoJsonParserResult;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.itu.data.models.parser.ParserGeoJsonElement.styleBelowWater;

public class GeoJsonService {
    private static GeoJsonService instance;

    // Fallback in-memory storage
    private List<GeoJsonElement> geoJsonElementsToBeDrawn = null;

    // General in-memory info
    private GeoJsonElement root;
    private Map<GeoJsonElement, List<GeoJsonElement>> connections;
    private Float maxWaterLevel, minWaterLevel;

    public static GeoJsonService getInstance() {
        if (instance == null) {
            instance = new GeoJsonService();
        }
        return instance;
    }

    public final GeoJsonDatabaseService geoJsonDatabaseService;

    private GeoJsonService() {
        geoJsonDatabaseService = GeoJsonDatabaseService.getInstance();
    }

    public float getMinWaterLevel() {
        if (minWaterLevel == null) {
            minWaterLevel = getGeoJsonElements().stream().min((e1,e2) -> (int) ((e1.getHeight()-e2.getHeight())*1000)).orElse(new ParserGeoJsonElement(0, new Path2D.Double())).getHeight();
        }
        return minWaterLevel;
    }

    public float getMaxWaterLevel() {
        if (maxWaterLevel == null) {
            maxWaterLevel = getGeoJsonElements().stream().max((e1,e2) -> (int) ((e1.getHeight()-e2.getHeight())*1000)).orElse(new ParserGeoJsonElement(0, new Path2D.Double())).getHeight();
        }
        return maxWaterLevel;
    }

    public List<GeoJsonElement> getGeoJsonElements() {
        if (geoJsonElementsToBeDrawn == null) {
            // Use DB
            return geoJsonDatabaseService.getGeoJsonElements();
        } else {
            // Use in-memory
            return geoJsonElementsToBeDrawn;
        }
    }

    public List<GeoJsonElement> getGeoJsonElementsToBeDrawn(float waterLevel) {
        if (geoJsonElementsToBeDrawn == null) {
            // Use DB
            return geoJsonDatabaseService.getGeoJsonElements();
        } else {
            // Use in-memory

            // Reset styles
            geoJsonElementsToBeDrawn.parallelStream().forEach(e -> e.setStyle(null));

            checkWaterLevelForElements(root, waterLevel);

            return geoJsonElementsToBeDrawn;
        }
    }

    public void checkWaterLevelForElements(GeoJsonElement parentElement, float waterLevel) {
        for (GeoJsonElement e : connections.get(parentElement)) {
            if (e.getHeight() <= waterLevel) {
                e.setStyle(styleBelowWater);
                checkWaterLevelForElements(e, waterLevel);
            }

        }
    }

    public void addGeoJsonParserResultInDatabase(GeoJsonParserResult geoJsonParserResult) {
        if (geoJsonDatabaseService.connectionEstablished()) {
            // Add elements to DB
            geoJsonDatabaseService.insertGeoJsonElementsInDb(geoJsonParserResult.getGeoJsonElements());
        } else {
            // Can't add to DB => use memory
            root = geoJsonParserResult.getRoot();

            connections = geoJsonParserResult.getConnections().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> (GeoJsonElement) entry.getKey(),                  // Cast key to superclass
                            entry -> entry.getValue().stream()
                                    .map(item -> (GeoJsonElement) item)          // Cast each list item to superclass
                                    .collect(Collectors.toList())    // Collect to new list
                    ));
            geoJsonElementsToBeDrawn = new ArrayList<>(geoJsonParserResult.getGeoJsonElements());
        }
    }
}
