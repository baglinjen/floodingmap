package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.dto.OsmParserResult;
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
}
