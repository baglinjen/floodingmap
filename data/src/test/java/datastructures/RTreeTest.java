package datastructures;

import dk.itu.common.models.WithBoundingBoxAndArea;
import dk.itu.data.datastructure.rtree.RStarTree;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.datastructure.rtree.RTreeNode;

import java.lang.reflect.Method;
import java.util.List;

import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.services.Services;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RTreeTest {
    static List<OsmNode> nodes;
    static RStarTree rStarTree;

    @BeforeAll
    public static void setUp() {
        rStarTree = new RStarTree();

        Services.withServices(services -> {
            services.getOsmService(false).loadOsmData("bornholm.osm");
            nodes = services.getOsmService(false).getTraversableOsmNodes();
        });
    }

    @AfterEach
    public void setUpIndividual(){
        rStarTree.clear();
    }

    @Test
    public void testInsertSingleElementCreatesRoot() {
        // Arrange
        OsmElement element = createOsmElement(1, new float[]{1, 1, 2, 2});

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
        OsmElement inside1 = createOsmElement(1, new float[]{1, 1, 2, 2}); // Area = 1
        OsmElement inside2 = createOsmElement(2, new float[]{1, 1, 3, 3}); // Area = 4
        OsmElement outside = createOsmElement(3, new float[]{10, 10, 12, 12});

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

        WithBoundingBoxAndArea overlapBox = new WithBoundingBoxAndArea() {
            @Override
            public float getArea() {
                return WithBoundingBoxAndArea.calculateArea(1.5f, 1.5f, 2.5f, 2.5f);
            }

            @Override
            public float minLon() {
                return 1.5f;
            }

            @Override
            public float minLat() {
                return 1.5f;
            }

            @Override
            public float maxLon() {
                return 2.5f;
            }

            @Override
            public float maxLat() {
                return 2.5f;
            }
        };

        child1.addEntry(createOsmElement(1, new float[]{0, 0, 2, 2}));
        child2.addEntry(createOsmElement(2, new float[]{5, 5, 6, 6}));
        root.getChildren().add(child1);
        root.getChildren().add(child2);

        Method chooseLeaf = RStarTree.class.getDeclaredMethod("chooseLeaf", RTreeNode.class, WithBoundingBoxAndArea.class);
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
        OsmNode node = new OsmNode(1, 1.5f, 1.5f, 0);
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
        OsmNode node1 = new OsmNode(1, 0.0f, 0.0f, 0);
        OsmNode node2 = new OsmNode(2, 10.0f, 10.0f, 0);
        OsmNode node3 = new OsmNode(3, 5.0f, 5.0f, 0);
        OsmNode node4 = new OsmNode(4, -5.0f, -5.0f, 0);

        rStarTree.insert(node1);
        rStarTree.insert(node2);
        rStarTree.insert(node3);
        rStarTree.insert(node4);

        // Act

        // Test point at origin - should find node1
        OsmNode nearest1 = rStarTree.getNearest(0.0f, 0.0f);

        // Test point near node2 - should find node2
        OsmNode nearest2 = rStarTree.getNearest(9.5f, 9.5f);

        // Test point equidistant from multiple nodes
        OsmNode nearest3 = rStarTree.getNearest(5.0f, 0.0f);

        // Test point near node4
        OsmNode nearest4 = rStarTree.getNearest(-4.0f, -4.0f);

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
                OsmNode node = new OsmNode(i * 10 + j, i, j, 0);
                rStarTree.insert(node);
            }
        }

        // Act
        // Test exact position
        OsmNode nearest1 = rStarTree.getNearest(5, 5);

        // Test position between grid points
        OsmNode nearest2 = rStarTree.getNearest(5.6f, 7.4f);

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
        OsmNode node1 = new OsmNode(1, 0, 0, 0);
        OsmNode node2 = new OsmNode(2, 10, 10, 0);

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
        OsmNode node1 = new OsmNode(1, 0, 0, 0);
        OsmNode node2 = new OsmNode(2, 10, 10, 0);

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
       OsmNode node1 = new OsmNode(1, 0, 0, 0);
       OsmNode node2 = new OsmNode(2, 10, 10, 0);

       rStarTree.insert(node1);
       rStarTree.insert(node2);

       // Act
       RTreeNode bbox = rStarTree.getRoot();

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

        child1.addEntry(createOsmElement(0, new float[]{0, 0, 5, 5}));
        child2.addEntry(createOsmElement(0, new float[]{10, 10, 15, 15}));

        root.getChildren().add(child1);
        root.getChildren().add(child2);

        // closer to child2
        RTreeNode testMbr = new RTreeNode() {
            @Override
            public float minLon() {
                return 11;
            }

            @Override
            public float minLat() {
                return 11;
            }

            @Override
            public float maxLon() {
                return 12;
            }

            @Override
            public float maxLat() {
                return 12;
            }

            @Override
            public float getArea() {
                return WithBoundingBoxAndArea.calculateArea(11, 11, 12, 12);
            }
        };

        Method method = RStarTree.class.getDeclaredMethod("findTargetNodeAtLevel", RTreeNode.class, RTreeNode.class, int.class);
        method.setAccessible(true);

        // Act
        RTreeNode result = (RTreeNode) method.invoke(tree, root, testMbr, 1);

        // Assertion: child2 should be selected based on minimal enlargement
        assertSame(child2, result);
    }

    private OsmElement createOsmElement(long id, float[] bounds) {
        return new OsmElement() {
            @Override
            public float getArea() {
                return WithBoundingBoxAndArea.calculateArea(bounds[0], bounds[1], bounds[2], bounds[3]);
            }

            @Override
            public float minLon() {
                return bounds[0];
            }

            @Override
            public float minLat() {
                return bounds[1];
            }

            @Override
            public float maxLon() {
                return bounds[2];
            }

            @Override
            public float maxLat() {
                return bounds[3];
            }

            @Override
            public long getId() {
                return id;
            }
        };
    }
}