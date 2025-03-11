package dk.itu.data.services;

import dk.itu.common.models.osm.OsmElement;
import dk.itu.data.dto.OsmParserResult;

import java.util.List;
import java.util.stream.Collectors;

public class OsmService {
    private static OsmService instance;

    // Fallback in-memory storage
    private List<OsmElement> osmElementsToBeDrawn = null;

    public static OsmService getInstance() {
        if (instance == null) {
            instance = new OsmService();
        }
        return instance;
    }


    public final OsmDatabaseService osmDatabaseService;
    private double[] bounds;

    private OsmService() {
        osmDatabaseService = OsmDatabaseService.getInstance();
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit) {
        if (osmElementsToBeDrawn == null) {
            // Use DB
            return osmDatabaseService.getOsmElements();
        } else {
            // Use in-memory
            return osmElementsToBeDrawn.stream().limit(limit).collect(Collectors.toList());
        }
    }

    public void addOsmParserResultInDatabase(OsmParserResult osmParserResult) {
        if (osmDatabaseService.connectionEstablished()) {
            // Add elements to DB
            osmDatabaseService.insertOsmElementsInDb(osmParserResult.getElementsToBeDrawn());
        } else {
            // Can't add to DB => use memory
            osmElementsToBeDrawn = osmParserResult.getElementsToBeDrawn();
            bounds = osmParserResult.getBounds();
        }
    }

    public double getMinLat() {
        if (osmDatabaseService.connectionEstablished()) {
            return osmDatabaseService.getMinLat();
        } else {
            return bounds[0];
        }
    }
    public double getMinLon(){
        if (osmDatabaseService.connectionEstablished()) {
            return osmDatabaseService.getMinLon();
        } else {
            return bounds[1];
        }
    }
    public double getMaxLat() {
        if (osmDatabaseService.connectionEstablished()) {
            return osmDatabaseService.getMaxLat();
        } else {
            return bounds[2];
        }
    }
    public double getMaxLon() {
        if (osmDatabaseService.connectionEstablished()) {
            return osmDatabaseService.getMaxLon();
        } else {
            return bounds[3];
        }
    }
}
