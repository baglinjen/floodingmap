package dk.itu.data.services;

import dk.itu.common.models.Drawable;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.data.repositories.OsmElementRepositoryDb;
import dk.itu.data.repositories.OsmElementRepositoryMemory;
import dk.itu.util.LoggerFactory;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.util.List;

public class OsmService {
    private static final float OSM_ELEMENT_PERCENT_SCREEN = 0.02f * 0.02f;
    private static final Logger logger = LoggerFactory.getLogger();
    private final OsmElementRepository osmElementRepository;

    public OsmService() {
        osmElementRepository = OsmElementRepositoryMemory.getInstance();
    }
    public OsmService(Connection connection) {
        osmElementRepository = new OsmElementRepositoryDb(connection);
    }

    public OsmNode getNearestTraversableOsmNode(float lon, float lat) {
        return osmElementRepository.getNearestTraversableOsmNode(lon, lat);
    }

    public void getOsmElementsToBeDrawnScaled(float minLon, float minLat, float maxLon, float maxLat, Float2ReferenceMap<Drawable> osmElements) {
        osmElementRepository.getOsmElementsScaled(minLon, minLat, maxLon, maxLat, (maxLon - minLon) * (maxLat - minLat) * OSM_ELEMENT_PERCENT_SCREEN, osmElements);
    }

    public List<OsmNode> getTraversableOsmNodes(){
        return osmElementRepository.getTraversableOsmNodes();
    }

    public List<RTreeNode> getSpatialNodes() {
        return osmElementRepository.getSpatialNodes();
    }

    public void loadOsmData(String osmFileName) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse(osmFileName, osmParserResult);

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        synchronized (this.osmElementRepository) {
            logger.info("Started inserting drawable elements to repository");
            long startTime = System.currentTimeMillis();
            osmElementRepository.add(osmParserResult.getElementsToBeDrawn());
            osmParserResult.clearElementsToBeDrawn();
            logger.info("Finished inserting drawable elements to repository in {} ms", System.currentTimeMillis() - startTime);
            logger.info("Started inserting traversable elements to repository");
            startTime = System.currentTimeMillis();
            osmElementRepository.addTraversable(osmParserResult.getTraversals());
            osmParserResult.clearTraversals();
            logger.info("Finished inserting traversable elements to repository in {} ms", System.currentTimeMillis() - startTime);
        }
    }

    public float[] getBounds() {
        return osmElementRepository.getBounds();
    }

    public void clearAll() {
        osmElementRepository.clearAll();
    }
}