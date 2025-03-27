package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.db.DbBounds;
import dk.itu.data.models.db.DbNode;
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

    public List<OsmElement> getOsmNodes(){
        var x = osmElementRepository.getOsmNodes();
        return x.stream().filter(element -> element instanceof DbNode).toList();//TODO: Reduce use of 'instanceof'?
    }

    public void loadOsmDataInDb(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        //Add nodes to database for routing
        osmElementRepository.add(osmParserResult.getNodesForRouting());

        // Filter and sort data for visual purposes
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
