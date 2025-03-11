package dk.itu.data.services;

import dk.itu.common.models.geojson.GeoJsonElement;
import dk.itu.data.dto.GeoJsonParserResult;

import java.awt.geom.Path2D;
import java.util.List;

public class GeoJsonService {
    private static GeoJsonService instance;

    // Fallback in-memory storage
    private List<GeoJsonElement> geoJsonElementsToBeDrawn = null;

    // General in-memory info
    private Float maxWaterLevel;

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

    public float getMaxWaterLevel() {
        if (maxWaterLevel == null) {
            maxWaterLevel = getGeoJsonElementsToBeDrawn().stream().max((e1,e2) -> (int) ((e1.getHeight()-e2.getHeight())*1000)).orElse(new GeoJsonElement(0, new Path2D.Double())).getHeight();
        }
        return maxWaterLevel;
    }

    public List<GeoJsonElement> getGeoJsonElementsToBeDrawn() {
        if (geoJsonElementsToBeDrawn == null) {
            // Use DB
            return geoJsonDatabaseService.getGeoJsonElements();
        } else {
            // Use in-memory
            return geoJsonElementsToBeDrawn;
        }
    }

    public void addGeoJsonParserResultInDatabase(GeoJsonParserResult geoJsonParserResult) {
        if (geoJsonDatabaseService.connectionEstablished()) {
            // Add elements to DB
            geoJsonDatabaseService.insertGeoJsonElementsInDb(geoJsonParserResult.getGeoJsonElements());
        } else {
            // Can't add to DB => use memory
            geoJsonElementsToBeDrawn = geoJsonParserResult.getGeoJsonElements();
        }
    }
}
