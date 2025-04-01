package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.db.Bounds;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.repositories.IOsmElementRepository;
import dk.itu.data.repositories.OsmElementRepositoryMemory;

import java.sql.Connection;
import java.util.List;

public class OsmService {

    private final IOsmElementRepository osmElementRepository;

    public OsmService(Connection connection) {
        // osmElementRepository = new OsmElementRepository(connection);
        osmElementRepository = null;
    }

    public OsmService() {
        osmElementRepository = OsmElementRepositoryMemory.getInstance();
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        return osmElementRepository.getOsmElements(limit, minLon, minLat, maxLon, maxLat);
    }

    public List<OsmElement> getOsmNodes(){
       // return osmElementRepository.getOsmNodes();
        return List.of();
    }

    public void loadOsmData(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        //Add nodes to database for routing
        // osmElementRepository.add(osmParserResult.getNodesForRouting());

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        // Add to Database
        osmElementRepository.add(osmParserResult.getElementsToBeDrawn());
    }

    public Bounds getBounds() {
        return osmElementRepository.getBounds();
    }

    public void clearAll() {
        // osmElementRepository.clearAll();
    }
}
