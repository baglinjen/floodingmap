package dk.itu.data.repositories;

import dk.itu.common.models.OsmElement;
import dk.itu.data.datastructure.rtree.RTree;
import dk.itu.data.models.db.Bounds;
import dk.itu.data.models.memory.*;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import kotlin.Pair;

import java.util.List;

public class OsmElementRepositoryMemory implements IOsmElementRepository{
    private static OsmElementRepositoryMemory instance;
    private final RTree rtree = new RTree();

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
    public Bounds getBounds() {
        return new Bounds(10.37, 55.94, 10.5, 56); // TODO: Find actual lat/lon from rtree
    }
}
