package dk.itu.data.repositories;

import dk.itu.data.datastructure.rtree.RTree;
import dk.itu.data.models.db.*;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;

import java.util.List;

public class OsmElementRepositoryMemory implements OsmElementRepository {
    private static OsmElementRepositoryMemory instance;
    private RTree rtree = new RTree();

    public static OsmElementRepositoryMemory getInstance() {
        if (instance == null) {
            instance = new OsmElementRepositoryMemory();
        }

        return instance;
    }

    private OsmElementRepositoryMemory() {}

    @Override
    public void add(List<ParserOsmElement> osmElements) {
        for (ParserOsmElement osmElement : osmElements) {
            switch (osmElement) {
                case ParserOsmNode node -> rtree.insert(OsmNode.mapToOsmNode(node));
                case ParserOsmWay way -> rtree.insert(OsmWay.mapToOsmWay(way));
                case ParserOsmRelation relation -> rtree.insert(OsmRelation.mapToOsmRelation(relation));
                default -> {}
            }
        }
    }

    @Override
    public List<OsmElement> getOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        return rtree.search(minLon, minLat, maxLon, maxLat).parallelStream().toList();
    }

    @Override
    public List<OsmNode> getOsmNodes() {
        return rtree.getNodes();
    }

    @Override
    public void clearAll() {
        rtree = new RTree();
    }

    @Override
    public BoundingBox getBounds() {
        if (rtree.getBoundingBox() == null) {
            return new BoundingBox(-180, -90, 180, 90);
        } else {
            return rtree.getBoundingBox();
        }
    }
}
