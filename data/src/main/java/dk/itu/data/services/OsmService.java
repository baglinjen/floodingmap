package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.db.DbBounds;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.repositories.OsmElementRepository;

import java.sql.Connection;
import java.util.List;

public class OsmService {

    private final OsmElementRepository osmElementRepository;

    public OsmService(Connection connection) {
        osmElementRepository = new OsmElementRepository(connection);
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        return osmElementRepository.getOsmElements(limit, minLon, minLat, maxLon, maxLat);
    }

    public void loadOsmDataInDb(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        // Filter and sort data
        osmParserResult.sanitize();

        // Add to Database
        osmElementRepository.add(osmParserResult.getElementsToBeDrawn());
    }

    public DbBounds getBounds() {
        return osmElementRepository.getBounds();
    }

    public void clearAll() {
        osmElementRepository.clearAll();
    }
}
