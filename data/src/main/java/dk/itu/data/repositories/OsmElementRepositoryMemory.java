package dk.itu.data.repositories;

import dk.itu.data.datastructure.rtree.RStarTree;
import dk.itu.data.models.db.*;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.db.osm.OsmRelation;
import dk.itu.data.models.db.osm.OsmWay;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
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
    public void add(List<ParserOsmElement> osmElements) {
        final int[] elementsAdded = {0};
        int elementsToAdd = osmElements.size();
        osmElements.parallelStream().map(this::mapToOsmElement).toList().forEach(element -> {
            rtree.insert(element);
            elementsAdded[0]++;
            if (elementsAdded[0] % 100_000 == 0) logger.debug("Added {}/{} osm elements", elementsAdded[0], elementsToAdd);
        });
    }

    private OsmElement mapToOsmElement(ParserOsmElement osmElement) {
        return switch (osmElement) {
            case ParserOsmNode node -> OsmNode.mapToOsmNode(node);
            case ParserOsmWay way -> OsmWay.mapToOsmWay(way);
            case ParserOsmRelation relation -> OsmRelation.mapToOsmRelation(relation);
            default -> null;
        };
    }

    @Override
    public void addTraversable(List<ParserOsmNode> nodes) {
        final int[] elementsAdded = {0};
        int elementsToAdd = nodes.size();
        nodes.parallelStream().map(OsmNode::mapToOsmNode).toList().forEach(element -> {
            traversable.insert(element);
            elementsAdded[0]++;
            if (elementsAdded[0] % 100_000 == 0) logger.debug("Added {}/{} traversable elements", elementsAdded[0], elementsToAdd);
        });
    }

    @Override
    public List<OsmElement> getOsmElementsScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea) {
        return rtree.searchScaled(minLon, minLat, maxLon, maxLat, minBoundingBoxArea);
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
