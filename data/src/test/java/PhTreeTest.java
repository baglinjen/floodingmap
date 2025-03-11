import dk.itu.common.models.osm.OsmElement;
import dk.itu.common.models.osm.OsmNode;
import dk.itu.common.models.osm.OsmWay;
import dk.itu.data.datastructure.PhTree;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PhTreeTest {

    @Test
    public void testPhTreePut() {
        var tree = new PhTree<OsmElement>();

        var node1 = new OsmNode(0, 15, 25);
        var node2 = new OsmNode(0, 15, 24);
        var way1 = new OsmWay(0, List.of(node1, node2));
        tree.put(way1);

        var node3 = new OsmNode(0, 13, 24);
        var node4 = new OsmNode(0, 12, 24);
        var way2 = new OsmWay(0, List.of(node3, node4));
        tree.put(way2);
    }
}
