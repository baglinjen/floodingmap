package dk.itu.data.services;

import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.OsmElement;
import dk.itu.data.models.db.OsmNode;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.data.repositories.OsmElementRepositoryDb;
import dk.itu.data.repositories.OsmElementRepositoryMemory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OsmService {

    private final OsmElementRepository osmElementRepository;

    public OsmService() {
        osmElementRepository = OsmElementRepositoryMemory.getInstance();
    }
    public OsmService(Connection connection) {
        osmElementRepository = new OsmElementRepositoryDb(connection);
    }

    public OsmNode getNearestTraversableOsmNode(double lon, double lat) {
        return osmElementRepository.getNearestTraversableOsmNode(lon, lat);
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        return osmElementRepository.getOsmElements(limit, minLon, minLat, maxLon, maxLat);
    }

    public List<OsmElement> getTraversableOsmNodes(){
        return new ArrayList<>(osmElementRepository.getTraversableOsmNodes());
    }

    public void loadOsmData(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        // Add to Database
        osmElementRepository.add(osmParserResult.getElementsToBeDrawn());
        osmElementRepository.addTraversable(osmParserResult.getTraversableNodes());
    }

    public BoundingBox getBounds() {
        return osmElementRepository.getBounds();
    }

    public void clearAll() {
         osmElementRepository.clearAll();
    }
}
