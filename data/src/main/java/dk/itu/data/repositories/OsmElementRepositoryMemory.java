package dk.itu.data.repositories;

import dk.itu.data.datastructure.rtree.RStarTree;
import dk.itu.data.models.BoundingBox;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OsmElementRepositoryMemory implements OsmElementRepository {
    private static final Logger logger = LoggerFactory.getLogger();
    private static OsmElementRepositoryMemory instance;

    public static OsmElementRepositoryMemory getInstance() {
        if (instance == null) {
            instance = new OsmElementRepositoryMemory();
        }

        return instance;
    }

    private OsmElementRepositoryMemory() {}

    private final RStarTree rtree = new RStarTree(), traversable = new RStarTree();

    @Override
    public void add(List<OsmElement> osmElements) {
        int elementsAdded = 0;
        int elementsToAdd = osmElements.size();
        for (OsmElement osmElement : osmElements) {
            rtree.insert(osmElement);
            elementsAdded++;
            if (elementsAdded % 100_000 == 0) logger.debug("Added {}/{} osm elements", elementsAdded, elementsToAdd);
        }
    }

    @Override
    public void addTraversable(List<OsmNode> nodes) {
        int elementsAdded = 0;
        int elementsToAdd = nodes.size();
        for (OsmElement node : nodes) {
            traversable.insert(node);
            elementsAdded++;
            if (elementsAdded % 100_000 == 0) logger.debug("Added {}/{} traversable elements", elementsAdded, elementsToAdd);
        }
    }

    @Override
    public void getOsmElementsScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea, List<OsmElement> osmElements) {
        rtree.searchScaled(minLon, minLat, maxLon, maxLat, minBoundingBoxArea, osmElements);
    }

    @Override
    public List<BoundingBox> getSpatialNodes() {
        return rtree.getBoundingBoxes();
    }

    @Override
    public List<OsmNode> getTraversableOsmNodes() {
        return traversable.getNodes();
    }

    @Override
    public OsmNode getNearestTraversableOsmNode(double lon, double lat) {
        return traversable.getNearest(lon, lat);
    }

    @Override
    public void clearAll() {
        rtree.clear();
        traversable.clear();
    }

    @Override
    public double[] getBounds() {
        if (rtree.isEmpty()) {
            return new double[]{-180, -90, 180, 90};
        } else {
            return rtree.getRoot().getBoundingBoxWithArea();
        }
    }
}