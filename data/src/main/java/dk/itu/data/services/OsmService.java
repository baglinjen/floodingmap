package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.dto.OsmParserResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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


    private double[] bounds;

    private OsmService() {}

    public void withOsmServiceConsumer(Consumer<OsmServiceOperations> osmServiceOperationsConsumer) {
        try (OsmDatabaseService osmDatabaseService = new OsmDatabaseService()) {
            osmServiceOperationsConsumer.accept(osmDatabaseService::fetchAllOsmElements);
        } catch (Exception e) {
            osmServiceOperationsConsumer.accept(this::getOsmElementsToBeDrawn);
        }
    }

    public boolean areElementsInDatabase() {
        try (OsmDatabaseService osmDatabaseService = new OsmDatabaseService()) {
            return osmDatabaseService.areElementsInDatabase();
        } catch (SQLException e) {
            return false;
        }
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        if (osmElementsToBeDrawn == null) {
            // Use DB
            try (OsmDatabaseService osmDatabaseService = new OsmDatabaseService()) {
                return osmDatabaseService.fetchAllOsmElements(limit, minLon, minLat, maxLon, maxLat);
            } catch (SQLException e) {
                // Use in-memory
                return osmElementsToBeDrawn.stream().limit(limit).collect(Collectors.toList());
            }
        } else {
            // Use in-memory
            return osmElementsToBeDrawn.stream().limit(limit).collect(Collectors.toList());
        }
    }

    public void addOsmParserResultInDatabase(OsmParserResult osmParserResult) {
        if (osmElementsToBeDrawn == null) {
            try (OsmDatabaseService osmDatabaseService = new OsmDatabaseService()) {
                if (osmDatabaseService.insertOsmElementsInDb(osmParserResult.getElementsToBeDrawn())) {
                    // Elements added to DB
                    osmElementsToBeDrawn = null;
                } else {
                    // Can't add to DB => use memory
                    osmElementsToBeDrawn = new ArrayList<>(osmParserResult.getElementsToBeDrawn());
                }
            } catch (SQLException e) {
                osmElementsToBeDrawn = new ArrayList<>(osmParserResult.getElementsToBeDrawn());
            }
        } else {
            osmElementsToBeDrawn = new ArrayList<>(osmParserResult.getElementsToBeDrawn());
        }
        bounds = osmParserResult.getBounds();
    }

    public double getMinLat() {
        // TODO: Add bounds in DB
//        return bounds[0];
        return 54.96;
    }
    public double getMinLon(){
        // TODO: Add bounds in DB
//        return bounds[1];
        return 10.4005300;
    }
    public double getMaxLat() {
        // TODO: Add bounds in DB
//        return bounds[2];
        return 57;
    }
    public double getMaxLon() {
        // TODO: Add bounds in DB
//        return bounds[3];
        return 15.2;
    }

    public interface OsmServiceOperations {
        List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat);
    }
}
