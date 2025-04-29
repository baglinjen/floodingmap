package datastructures;

import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.datastructure.rtree.RTree;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.db.BoundingBox;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class RTreeTest {

    @Test
    public void testInsertSingleElementCreatesRoot() {
        // Arrange
        RTree rtree = new RTree();
        BoundingBox bbox = new BoundingBox(1, 1, 2, 2);
        OsmElement element = new OsmElement(1, bbox, bbox.area()) {
            @Override
            public void prepareDrawing(Graphics2D g2d) {}
            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };

        // Act
        rtree.insert(element);

        // Assert
        assertNotNull(rtree.getRoot(), "Root should not be null after first insert");
        assertEquals(1, rtree.getRoot().getElements().size(), "Root should contain one element");
        assertEquals(bbox.area(), rtree.getRoot().getMBR().area(), "Root MBR should match inserted element's bounding box area");
        assertTrue(rtree.getRoot().getElements().contains(element), "Inserted element should be in the root");
    }

    @Test
    public void testSearchReturnsOnlyMatchingElements() {
        // Arrange
        RTree rtree = new RTree();
        BoundingBox bbox1 = new BoundingBox(1, 1, 2, 2);
        BoundingBox bbox2 = new BoundingBox(1, 1, 3, 3);
        BoundingBox bbox3 = new BoundingBox(10, 10, 12, 12);
        // Elements to insert
        OsmElement inside1 = new OsmElement(1, bbox1, bbox1.area()) {
            @Override
            public void prepareDrawing(Graphics2D g2d) {}
            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        }; // Area = 1
        OsmElement inside2 = new OsmElement(2, bbox2, bbox2.area()) {
            @Override
            public void prepareDrawing(Graphics2D g2d) {}
            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        }; // Area = 4
        OsmElement outside = new OsmElement(3, bbox3, bbox3.area()) {
            @Override
            public void prepareDrawing(Graphics2D g2d) {}
            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };

        rtree.insert(inside1);
        rtree.insert(inside2);
        rtree.insert(outside);

        // Act
        List<OsmElement> results = rtree.search(0, 0, 5, 5);

        // Assert
        assertEquals(2, results.size(), "Should return only 2 matching elements");
        assertTrue(results.contains(inside1), "Result should contain inside1");
        assertTrue(results.contains(inside2), "Result should contain inside2");
        assertFalse(results.contains(outside), "Result should NOT contain outside");

        // Check order (by area descending)
        assertEquals(inside2, results.get(0), "Element with larger area should come first");
        assertEquals(inside1, results.get(1), "Element with smaller area should come second");
    }

    @Test
    public void testChooseLeafChoosesNodeWithLeastEnlargement() throws Exception {
        // Arrange
        RTree rtree = new RTree();

        RTreeNode root = new RTreeNode();
        RTreeNode child1 = new RTreeNode();
        RTreeNode child2 = new RTreeNode();

        BoundingBox bbox1 = new BoundingBox(0, 0, 2, 2);
        BoundingBox bbox2 = new BoundingBox(5, 5, 6, 6);
        BoundingBox overlapBox = new BoundingBox(1.5, 1.5, 2.5, 2.5);

        child1.setMBR(bbox1);
        child2.setMBR(bbox2);
        root.getChildren().add(child1);
        root.getChildren().add(child2);

        Method chooseLeaf = RTree.class.getDeclaredMethod("chooseLeaf", RTreeNode.class, BoundingBox.class);
        chooseLeaf.setAccessible(true);

        // Act
        RTreeNode result = (RTreeNode) chooseLeaf.invoke(rtree, root, overlapBox);

        // Assert
        assertEquals(child1, result, "Expected chooseLeaf to select child1 due to least enlargement");
    }
}
