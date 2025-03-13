import dk.itu.common.models.osm.OsmElement;
import dk.itu.common.models.osm.OsmNode;
import dk.itu.common.models.osm.OsmWay;
import dk.itu.data.datastructure.PhTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dk.itu.data.datastructure.PhNode.printLong;

public class PhTreeTest {

    @Test
    public void testPhTreePut() {
        var tree = new PhTree<OsmElement>();

        for (int i = 0; i < 10; i++) {
            generateAndPutWay(tree, i);
        }

    }

    private void generateAndPutWay(PhTree<OsmElement> tree, int i) {

        double lon1 = getRandomNumber(50, 60);
        double lon2 = getRandomNumber(50, 60);
        double lat1 = getRandomNumber(10, 20);
        double lat2 = getRandomNumber(10, 20);

        System.out.println("ELEMENT: " + i);
        printLong(Double.doubleToLongBits(lon1));
        printLong(Double.doubleToLongBits(lon2));
        printLong(Double.doubleToLongBits(lat1));
        printLong(Double.doubleToLongBits(lat2));
        System.out.println();

        var node1 = new OsmNode(0, lat1, lon1);
        var node2 = new OsmNode(0, lat2, lon2);
        var way = new OsmWay(i, List.of(node1, node2));
        tree.put(way);
    }

    private double getRandomNumber(int min, int max) {
        return ((Math.random() * (max - min)) + min);
    }
}
