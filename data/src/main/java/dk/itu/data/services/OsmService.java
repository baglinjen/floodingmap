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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmService {
    private static final double OSM_ELEMENT_PERCENT_SCREEN = 0.02 * 0.02;
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

    public List<OsmElement> getOsmElementsToBeDrawnScaled(double minLon, double minLat, double maxLon, double maxLat) {
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getOsmElementsScaled(minLon, minLat, maxLon, maxLat, (maxLon - minLon) * (maxLat - minLat) * OSM_ELEMENT_PERCENT_SCREEN);
        }
    }

    public List<BoundingBox> getBoundingBoxes() {
        synchronized (this.osmElementRepository) {
            return osmElementRepository.getBoundingBoxes();
        }
    }

    public Map<Long, OsmNode> getTraversableOsmNodes(){
        synchronized (this.osmElementRepository) {
            var nodes = osmElementRepository.getTraversableOsmNodes();
            var result = new HashMap<Long, OsmNode>();

            nodes.forEach(node -> result.put(node.getId(), node));
            return result;
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
