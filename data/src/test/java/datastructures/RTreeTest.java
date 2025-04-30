package datastructures;

import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.datastructure.rtree.RTree;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.db.BoundingBox;

import java.lang.reflect.Method;
import java.util.List;

import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.services.Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class RTreeTest {
    List<OsmNode> nodes;
    RTree rtree;

    @BeforeEach
    public void setUp() {
        rtree = new RTree();

        Services.withServices(services -> {
            services.getOsmService(false).loadOsmData("tuna.osm");
            services.getHeightCurveService().loadGmlFileData("tuna-dijkstra.gml");
            nodes = services.getOsmService(false).getTraversableOsmNodes();
        });
    }

    @Test
    public void testInsertSingleElementCreatesRoot() {
        // Arrange
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

    @Test
    public void testNearestEmptyTree() {
        // Test nearest neighbor on empty tree
        OsmNode nearest = rtree.getNearest(0, 0);
        assertNull(nearest, "Empty tree should return null for nearest neighbor");
    }

    @Test
    public void testNearestSingleNode() {
        // Arrange
        BoundingBox bbox = new BoundingBox(1, 1, 2, 2);

        OsmNode node = new OsmNode(1, 1.5, 1.5, bbox, null);
        rtree.insert(node);

        // Act
        OsmNode nearest = rtree.getNearest(1, 2);

        // Assert
        assertNotNull(nearest, "Should find the only available node");
        assertEquals(1, nearest.getId(), "Should find the node with ID 1");
    }

    @Test
    public void testNearestMultipleNodes() {
        // Arrange
        BoundingBox bbox1 = new BoundingBox(0, 0, 0, 0);
        BoundingBox bbox2 = new BoundingBox(10.0, 10.0, 10.0, 10.0);
        BoundingBox bbox3 = new BoundingBox(5.0, 5.0, 5.0, 5.0);
        BoundingBox bbox4 = new BoundingBox(-5.0, -5.0, -5.0, -5.0);

        OsmNode node1 = new OsmNode(1, 0.0, 0.0, bbox1, null);
        OsmNode node2 = new OsmNode(2, 10.0, 10.0, bbox2, null);
        OsmNode node3 = new OsmNode(3, 5.0, 5.0, bbox3, null);
        OsmNode node4 = new OsmNode(4, -5.0, -5.0, bbox4, null);

        rtree.insert(node1);
        rtree.insert(node2);
        rtree.insert(node3);
        rtree.insert(node4);

        // Act

        // Test point at origin - should find node1
        OsmNode nearest1 = rtree.getNearest(0.0, 0.0);

        // Test point near node2 - should find node2
        OsmNode nearest2 = rtree.getNearest(9.5, 9.5);

        // Test point equidistant from multiple nodes
        OsmNode nearest3 = rtree.getNearest(5.0, 0.0);

        // Test point near node4
        OsmNode nearest4 = rtree.getNearest(-4.0, -4.0);

        // Assert
        assertEquals(node1.getId(), nearest1.getId(), "Should find node1 at the origin");
        assertEquals(node2.getId(), nearest2.getId(), "Should find node2 as the nearest");
        assertNotNull(nearest3, "Should find a node even at equidistant point");
        assertEquals(node4.getId(), nearest4.getId(), "Should find node4 as the nearest");
    }

    @Test
    public void testGridOfNodes() {
        // Arrange
        for (int i = 0; i < 10; i++) {   // 10x10 grid of nodes
            for (int j = 0; j < 10; j++) {
                OsmNode node = new OsmNode(i * 10 + j, i, j, new BoundingBox(i, j, i, j), null);
                rtree.insert(node);
            }
        }

        // Act
        // Test exact position
        OsmNode nearest1 = rtree.getNearest(5, 5);

        // Test position between grid points
        OsmNode nearest2 = rtree.getNearest(5.6, 7.4);

        // Test position outside grid but closest to a corner
        OsmNode nearest3 = rtree.getNearest(-1, -1);

        // Position far away
        OsmNode nearest4 = rtree.getNearest(100, 100);

        // Assert
        assertEquals(55, nearest1.getId(), "Should find node at (5,5)");
        assertEquals(67, nearest2.getId(), "Should find node at (6,7)");
        assertEquals(0, nearest3.getId(), "Should find node at (0,0)");
        assertEquals(99, nearest4.getId(), "Should find node at (9,9)");
    }

    @Test
    public void testGetElements() {
        OsmNode node1 = new OsmNode(1, 0, 0, new BoundingBox(0, 0, 0, 0), null);
        OsmNode node2 = new OsmNode(2, 10, 10, new BoundingBox(10, 10, 10, 10), null);

        rtree.insert(node1);
        rtree.insert(node2);

        List<OsmNode> elements = rtree.getElements();
        assertEquals(2, elements.size(), "Should return all added nodes");
        assertTrue(elements.stream().anyMatch(n -> n.getId() == 1), "Should contain node1");
        assertTrue(elements.stream().anyMatch(n -> n.getId() == 2), "Should contain node2");
    }

    @Test
    public void testEmptyElements()
    {
        // Elements should be empty at initialization, before insertion.
        assertTrue(rtree.getElements().isEmpty(), "Empty tree should return empty list");
    }
}
