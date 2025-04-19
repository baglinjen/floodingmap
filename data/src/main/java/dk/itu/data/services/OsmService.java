package dk.itu.data.services;

import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.data.repositories.OsmElementRepositoryDb;
import dk.itu.data.repositories.OsmElementRepositoryMemory;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.util.List;

public class OsmService {
    private static final Logger logger = LoggerFactory.getLogger();
    private final OsmElementRepository osmElementRepository;

    public OsmService() {
        osmElementRepository = OsmElementRepositoryMemory.getInstance();
    }
    public OsmService(Connection connection) {
        osmElementRepository = new OsmElementRepositoryDb(connection);
    }

    public OsmNode getNearestTraversableOsmNode(double lon, double lat) {
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getNearestTraversableOsmNode(lon, lat);
        }
    }

    public List<OsmElement> getOsmElementsToBeDrawn(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getOsmElements(limit, minLon, minLat, maxLon, maxLat);
        }
    }

    public List<OsmNode> getTraversableOsmNodes(){
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getTraversableOsmNodes();
        }
    }

    public void loadOsmData(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        synchronized (this.osmElementRepository) {
            // Add to Database
            logger.info("Started inserting drawable elements to repository");
            osmElementRepository.add(osmParserResult.getElementsToBeDrawn());
            logger.info("Finished inserting drawable elements to repository");
            logger.info("Started inserting traversable elements to repository");
            osmElementRepository.addTraversable(osmParserResult.getTraversableNodes());
            logger.info("Finished inserting traversable elements to repository");
        }
    }

    public BoundingBox getBounds() {
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getBounds();
        }
    }

    public void clearAll() {
        synchronized (this.osmElementRepository) {
            osmElementRepository.clearAll();
        }
    }
}
