package datastructures;

import dk.itu.data.datastructure.rtree.RStarTree;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.BoundingBox;

import java.lang.reflect.Method;
import java.util.List;

import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.services.Services;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class RTreeTest {
    static List<OsmNode> nodes;
    static RStarTree rStarTree;

    @BeforeAll
    public static void setUp() {
        rStarTree = new RStarTree();

        Services.withServices(services -> {
            services.getOsmService(false).loadOsmData("bornholm.osm");
            nodes = services.getOsmService(false).getTraversableOsmNodes().values().stream().toList();
        });
    }

    @AfterEach
    public void setUpIndividual(){
        rStarTree.clear();
    }

    @Test
    public void testInsertSingleElementCreatesRoot() {
        // Arrange
        OsmElement element = new OsmElement(1, new double[]{1, 1, 2, 2}) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };

        // Act
        rStarTree.insert(element);

        // Assert
        assertNotNull(rStarTree.getRoot(), "Root should not be null after first insert");
        assertEquals(1, rStarTree.getRoot().getElements().size(), "Root should contain one element");
        assertEquals(element.getArea(), rStarTree.getRoot().getArea(), "Root MBR should match inserted element's bounding box area");
        assertTrue(rStarTree.getRoot().getElements().contains(element), "Inserted element should be in the root");
    }

    @Test
    public void testSearchReturnsOnlyMatchingElements() {
        // Arrange
        // Elements to insert
        OsmElement inside1 = new OsmElement(1, new double[]{1, 1, 2, 2}) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        }; // Area = 1
        OsmElement inside2 = new OsmElement(2, new double[]{1, 1, 3, 3}) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        }; // Area = 4
        OsmElement outside = new OsmElement(3, new double[]{10, 10, 12, 12}) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };

        rStarTree.insert(inside1);
        rStarTree.insert(inside2);
        rStarTree.insert(outside);

        // Act
        List<OsmElement> results = rStarTree.search(0, 0, 5, 5);

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
        RTreeNode root = new RTreeNode();
        RTreeNode child1 = new RTreeNode();
        RTreeNode child2 = new RTreeNode();

        BoundingBox bbox1 = new BoundingBox(0, 0, 2, 2) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };
        BoundingBox bbox2 = new BoundingBox(5, 5, 6, 6) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };
        BoundingBox overlapBox = new BoundingBox(1.5, 1.5, 2.5, 2.5) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {}
        };

        child1.setBoundingBox(bbox1);
        child2.setBoundingBox(bbox2);
        root.getChildren().add(child1);
        root.getChildren().add(child2);

        Method chooseLeaf = RStarTree.class.getDeclaredMethod("chooseLeaf", RTreeNode.class, BoundingBox.class);
        chooseLeaf.setAccessible(true);

        // Act
        RTreeNode result = (RTreeNode) chooseLeaf.invoke(rStarTree, root, overlapBox);

        // Assert
        assertEquals(child1, result, "Expected chooseLeaf to select child1 due to least enlargement");
    }

    @Test
    public void testNearestEmptyTree() {
        // Test nearest neighbor on empty tree
        OsmNode nearest = rStarTree.getNearest(0, 0);
        assertNull(nearest, "Empty tree should return null for nearest neighbor");
    }

    @Test
    public void testNearestSingleNode() {
        // Arrange
        OsmNode node = new OsmNode(1, 1.5, 1.5, new double[]{1, 1, 2, 2}, null);
        rStarTree.insert(node);

        // Act
        OsmNode nearest = rStarTree.getNearest(1, 2);

        // Assert
        assertNotNull(nearest, "Should find the only available node");
        assertEquals(1, nearest.getId(), "Should find the node with ID 1");
    }

    @Test
    public void testNearestMultipleNodes() {
        // Arrange
        OsmNode node1 = new OsmNode(1, 0.0, 0.0, new double[]{0, 0, 0, 0}, null);
        OsmNode node2 = new OsmNode(2, 10.0, 10.0, new double[]{10, 10, 10, 10}, null);
        OsmNode node3 = new OsmNode(3, 5.0, 5.0, new double[]{5, 5, 5, 5}, null);
        OsmNode node4 = new OsmNode(4, -5.0, -5.0, new double[]{-5, -5, -5, -5}, null);

        rStarTree.insert(node1);
        rStarTree.insert(node2);
        rStarTree.insert(node3);
        rStarTree.insert(node4);

        // Act

        // Test point at origin - should find node1
        OsmNode nearest1 = rStarTree.getNearest(0.0, 0.0);

        // Test point near node2 - should find node2
        OsmNode nearest2 = rStarTree.getNearest(9.5, 9.5);

        // Test point equidistant from multiple nodes
        OsmNode nearest3 = rStarTree.getNearest(5.0, 0.0);

        // Test point near node4
        OsmNode nearest4 = rStarTree.getNearest(-4.0, -4.0);

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
                OsmNode node = new OsmNode(i * 10 + j, i, j, new double[]{i, j, i, j}, null);
                rStarTree.insert(node);
            }
        }

        // Act
        // Test exact position
        OsmNode nearest1 = rStarTree.getNearest(5, 5);

        // Test position between grid points
        OsmNode nearest2 = rStarTree.getNearest(5.6, 7.4);

        // Test position outside grid but closest to a corner
        OsmNode nearest3 = rStarTree.getNearest(-1, -1);

        // Position far away
        OsmNode nearest4 = rStarTree.getNearest(100, 100);

        // Assert
        assertEquals(55, nearest1.getId(), "Should find node at (5,5)");
        assertEquals(67, nearest2.getId(), "Should find node at (6,7)");
        assertEquals(0, nearest3.getId(), "Should find node at (0,0)");
        assertEquals(99, nearest4.getId(), "Should find node at (9,9)");
    }

    @Test
    public void testGetElements() {
        OsmNode node1 = new OsmNode(1, 0, 0, new double[]{0, 0, 0, 0}, null);
        OsmNode node2 = new OsmNode(2, 10, 10, new double[]{10, 10, 10, 10}, null);

        rStarTree.insert(node1);
        rStarTree.insert(node2);

        List<OsmNode> elements = rStarTree.getNodes();
        assertEquals(2, elements.size(), "Should return all added nodes");
        assertTrue(elements.stream().anyMatch(n -> n.getId() == 1), "Should contain node1");
        assertTrue(elements.stream().anyMatch(n -> n.getId() == 2), "Should contain node2");
    }

    @Test
    public void testEmptyElements()
    {
        // Elements should be empty at initialization, before insertion.
        assertTrue(rStarTree.getNodes().isEmpty(), "Empty tree should return empty list");
    }

    @Test
    public void testGetRoot()
    {
        // Arrange
        OsmNode node1 = new OsmNode(1, 0, 0, new double[]{0, 0, 0, 0}, null);
        OsmNode node2 = new OsmNode(2, 10, 10, new double[]{10, 10, 10, 10}, null);

        rStarTree.insert(node1);
        rStarTree.insert(node2);

        // Act
        RTreeNode root = rStarTree.getRoot();

        // Assert
        assertNotNull(root);
    }

    @Test
    public void testGetBoundingBox() {
        // Arrange
       OsmNode node1 = new OsmNode(1, 0, 0, new double[]{0, 0, 0, 0}, null);
       OsmNode node2 = new OsmNode(2, 10, 10, new double[]{10, 10, 10, 10}, null);

       rStarTree.insert(node1);
       rStarTree.insert(node2);

       // Act
       BoundingBox bbox = rStarTree.getRoot();

       // Assert
       assertNotNull(bbox);
    }

    @Test
    public void testFindTargetNodeAtLevel_ChoosesChildWithMinEnlargement() throws Exception {
        // Arrange
        RStarTree tree = new RStarTree();

        RTreeNode root = new RTreeNode();
        RTreeNode child1 = new RTreeNode();
        RTreeNode child2 = new RTreeNode();

        child1.setBoundingBox(new BoundingBox(0, 0, 5, 5) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {

            }
        });
        child2.setBoundingBox(new BoundingBox(10, 10, 15, 15) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {

            }
        });

        root.getChildren().add(child1);
        root.getChildren().add(child2);

        BoundingBox testMbr = new BoundingBox(11, 11, 12, 12) {

            @Override
            public void draw(Graphics2D g2d, float strokeBaseWidth) {

            }
        };  // closer to child2

        Method method = RStarTree.class.getDeclaredMethod("findTargetNodeAtLevel", RTreeNode.class, BoundingBox.class, int.class);
        method.setAccessible(true);

        // Act
        RTreeNode result = (RTreeNode) method.invoke(tree, root, testMbr, 1);

        // Assertion: child2 should be selected based on minimal enlargement
        assertSame(child2, result);
    }
}