package dk.itu.data.repositories;

import dk.itu.data.datastructure.rtree.RTree;
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

    private RTree rtree = new RTree();
    private RTree traversable = new RTree();

    @Override
    public synchronized void add(List<ParserOsmElement> osmElements) {
        final int[] elementsAdded = {0};
        int elementsToAdd = osmElements.size();
        osmElements.parallelStream().map(this::mapToOsmElement).toList().forEach(element -> {
            rtree.insert(element);
            elementsAdded[0]++;
            if (elementsAdded[0] % 10_000 == 0) logger.debug("Added {}/{} osm elements", elementsAdded[0], elementsToAdd);
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
    public synchronized void addTraversable(List<ParserOsmNode> nodes) {
        final int[] elementsAdded = {0};
        int elementsToAdd = nodes.size();
        nodes.parallelStream().map(OsmNode::mapToOsmNode).toList().forEach(element -> {
            traversable.insert(element);
            elementsAdded[0]++;
            if (elementsAdded[0] % 10_000 == 0) logger.debug("Added {}/{} traversable elements", elementsAdded[0], elementsToAdd);
        });
    }

    @Override
    public synchronized List<OsmElement> getOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        return rtree.search(minLon, minLat, maxLon, maxLat);
    }

    @Override
    public synchronized List<OsmNode> getTraversableOsmNodes() {
        return traversable.getElements();
    }

    @Override
    public synchronized OsmNode getNearestTraversableOsmNode(double lon, double lat) {
        return traversable.getNearest(lon, lat);
    }

    @Override
    public synchronized void clearAll() {
        rtree = new RTree();
        traversable = new RTree();
    }

    @Override
    public synchronized BoundingBox getBounds() {
        if (rtree.getBoundingBox() == null) {
            return new BoundingBox(-180, -90, 180, 90);
        } else {
            return rtree.getBoundingBox();
        }
    }
}
