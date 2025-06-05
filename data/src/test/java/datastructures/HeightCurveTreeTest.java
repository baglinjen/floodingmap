package datastructures;

import dk.itu.data.datastructure.heightcurvetree.HeightCurveTree;
import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HeightCurveTreeTest {
    @Test
    public void testWaterLevelMinMaxIsUpdatedCorrectly() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        HeightCurveElement heightCurveElementMin = HeightCurveElement.mapToHeightCurveElement(
                new ParserHeightCurveElement(
                        0,
                        new float[] {0, 0, 5, 5, 5, 0},
                        -5
                )
        );
        HeightCurveElement heightCurveElementMax = HeightCurveElement.mapToHeightCurveElement(
                new ParserHeightCurveElement(
                    0,
                    new float[] {1, 1, 4, 4, 4, 1},
                    5
                )
        );

        // Act
        heightCurveTree.put(heightCurveElementMin);
        heightCurveTree.put(heightCurveElementMax);

        // Assert
        assertThat(heightCurveTree.getMinWaterLevel()).isEqualTo(0); // Min water level is set to 0
        assertThat(heightCurveTree.getMaxWaterLevel()).isEqualTo(5);
    }

    @Test
    public void testGetElementsReturnsAllElementsInTree() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 1, 1, 1, 0},
                                1
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                2
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                3
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        List<HeightCurveElement> elementsList = new ArrayList<>();
        heightCurveTree.getElements(elementsList);
        assertThat(elementsList.size()).isEqualTo(elements.size() + 1); // +1 for root
    }

    @Test
    public void testFloodingStepsReturnsAllElements() {
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 1, 1, 1, 0},
                                1
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                2
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                3
                        )
                )
        );


        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        var floodingSteps = heightCurveTree.getFloodingSteps(4);
        var floodingStepsFlat = floodingSteps.stream().flatMap(Collection::stream).toList();
        assertThat(floodingStepsFlat.size()).isEqualTo(elements.size() + 1); // +1 for root
    }

    @Test
    public void testFloodingStepsReturnsAllElementsBelowWaterLevel() {
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 1, 1, 1, 0},
                                1
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                2
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                5
                        )
                )
        );


        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        var floodingSteps = heightCurveTree.getFloodingSteps(4);
        var floodingStepsFlat = floodingSteps.stream().flatMap(Collection::stream).toList();

        // +1 for root -1 for last element above water level
        assertThat(floodingStepsFlat.size()).isEqualTo(elements.size() + 1 - 1);
    }

    @Test
    public void testFloodingStepsReturnsCorrectNumberOfSteps() {
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Big element containing => should be flooded on step 2
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 100, 100, 100, 0},
                                1
                        )
                ),
                // #2 Element in #1 => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {1, 1, 6, 6, 6, 1},
                                2
                        )
                ),
                // #3 Element in #2 but lower => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                1
                        )
                ),
                // #4 Element in #2 => should be flooded on step 4
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                3
                        )
                ),
                // #5 Element in #1 => should not be flooded because it is higher that water level
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {101, 101, 102, 102, 102, 101},
                                10
                        )
                )
        );


        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert correct number of steps => 4
        assertThat(heightCurveTree.getFloodingSteps(5).size()).isEqualTo(4);
    }

    @Test
    public void testFloodingStepsEachStepContainsCorrectNumberOfElements() {
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Big element containing => should be flooded on step 2
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 100, 100, 100, 0},
                                1
                        )
                ),
                // #2 Element in #1 => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {1, 1, 6, 6, 6, 1},
                                2
                        )
                ),
                // #3 Element in #2 but lower => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                1
                        )
                ),
                // #4 Element in #2 => should be flooded on step 4
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                3
                        )
                ),
                // #5 Element in #1 => should not be flooded because it is higher that water level
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {101, 101, 102, 102, 102, 101},
                                10
                        )
                )
        );


        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }
        var floodingSteps = heightCurveTree.getFloodingSteps(5);

        // Assert that step max height is increasing
        var floodingSteps1MaxHeight = floodingSteps.getFirst().stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps2MaxHeight = floodingSteps.get(1).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps3MaxHeight = floodingSteps.get(2).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps4MaxHeight = floodingSteps.get(3).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();

        assertThat(floodingSteps1MaxHeight).isEqualTo(0); // Root height
        assertThat(floodingSteps2MaxHeight).isGreaterThan(floodingSteps1MaxHeight);
        assertThat(floodingSteps3MaxHeight).isGreaterThan(floodingSteps2MaxHeight);
        assertThat(floodingSteps4MaxHeight).isGreaterThan(floodingSteps3MaxHeight);
    }

    @Test
    public void testFloodingStepsMaxWaterLevelIncreases() {
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Big element containing => should be flooded on step 2
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 100, 100, 100, 0},
                                1
                        )
                ),
                // #2 Element in #1 => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {1, 1, 6, 6, 6, 1},
                                2
                        )
                ),
                // #3 Element in #2 but lower => should be flooded on step 3
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {2, 2, 3, 3, 3, 2},
                                1
                        )
                ),
                // #4 Element in #2 => should be flooded on step 4
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                3
                        )
                ),
                // #5 Element in #1 => should not be flooded because it is higher that water level
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {101, 101, 102, 102, 102, 101},
                                10
                        )
                )
        );


        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }
        var floodingSteps = heightCurveTree.getFloodingSteps(5);

        // Assert that step max height is increasing
        var floodingSteps1MaxHeight = floodingSteps.getFirst().stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps2MaxHeight = floodingSteps.get(1).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps3MaxHeight = floodingSteps.get(2).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();
        var floodingSteps4MaxHeight = floodingSteps.get(3).stream().max(Comparator.comparing(HeightCurveElement::getHeight)).orElseThrow().getHeight();

        assertThat(floodingSteps1MaxHeight).isEqualTo(0); // Root height
        assertThat(floodingSteps2MaxHeight).isGreaterThan(floodingSteps1MaxHeight);
        assertThat(floodingSteps3MaxHeight).isGreaterThan(floodingSteps2MaxHeight);
        assertThat(floodingSteps4MaxHeight).isGreaterThan(floodingSteps3MaxHeight);
    }

    @Test
    public void testGetHeightCurveForPointReturnsCorrectHeightCurve() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Element with height 1 as ID
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 10, 10, 10, 0},
                                1
                        )
                ),
                // #2 Element with height 2 as ID
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {20, 20, 30, 30, 30, 20},
                                2
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }
        var heightCurve = heightCurveTree.getHeightCurveForPoint(25, 25);

        // Assert
        assertThat(heightCurve.getHeight()).isEqualTo(2);
    }

    @Test
    public void testGetHeightCurveForPointReturnsRootIfNoneFound() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Element with height 1 as ID
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 10, 10, 10, 0},
                                1
                        )
                ),
                // #2 Element with height 2 as ID
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {20, 20, 30, 30, 30, 20},
                                2
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }
        var heightCurve = heightCurveTree.getHeightCurveForPoint(35, 35);

        // Assert
        assertThat(heightCurve.getHeight()).isEqualTo(0); // Should have selected root
    }

    @Test
    public void testInsertWithNonIntersectingElementsHasDepth1() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 10, 10, 10, 0},
                                1
                        )
                ),
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {20, 20, 30, 30, 30, 20},
                                1
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        var rootChildren = getChildren(getRoot(heightCurveTree));
        assertThat(rootChildren.size()).isEqualTo(2);
        assertThat(rootChildren).allSatisfy(child -> assertThat(getChildren(child)).isEmpty());
    }

    @Test
    public void testInsertWithElementContainedInElementHasDepth2AndRootHasOneChild() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Containing element
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 10, 10, 10, 0},
                                1
                        )
                ),
                // #2 Contained element
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                2
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        var rootChildren = getChildren(getRoot(heightCurveTree));
        assertThat(rootChildren.size()).isEqualTo(1);
        assertThat(rootChildren.getFirst().getHeight()).isEqualTo(1); // #1 Child containing

        var elementOneChildren = getChildren(rootChildren.getFirst());
        assertThat(elementOneChildren.size()).isEqualTo(1);
        assertThat(elementOneChildren.getFirst().getHeight()).isEqualTo(2); // #2 Child contained
    }

    @Test
    public void testInsertWithElementContainingElementHasDepth2AndRootHasOneChild() {
        // Arrange
        HeightCurveTree heightCurveTree = new HeightCurveTree();
        List<HeightCurveElement> elements = List.of(
                // #1 Contained element
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {4, 4, 5, 5, 5, 4},
                                2
                        )
                ),
                // #2 Containing element
                HeightCurveElement.mapToHeightCurveElement(
                        new ParserHeightCurveElement(
                                0,
                                new float[] {0, 0, 10, 10, 10, 0},
                                1
                        )
                )
        );

        // Act
        for (HeightCurveElement element : elements) { heightCurveTree.put(element); }

        // Assert
        var rootChildren = getChildren(getRoot(heightCurveTree));
        assertThat(rootChildren.size()).isEqualTo(1);
        assertThat(rootChildren.getFirst().getHeight()).isEqualTo(1); // #1 Child containing

        var elementOneChildren = getChildren(rootChildren.getFirst());
        assertThat(elementOneChildren.size()).isEqualTo(1);
        assertThat(elementOneChildren.getFirst().getHeight()).isEqualTo(2); // #2 Child contained
    }

    private HeightCurveElement getRoot(HeightCurveTree heightCurveTree) {
        try {
            var rootAccessor = HeightCurveTree.class.getDeclaredField("root");
            rootAccessor.setAccessible(true);
            return (HeightCurveElement) rootAccessor.get(heightCurveTree);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
            throw new TestAbortedException("Could not get root from Height Curve Tree", e);
        }
    }

    private List<HeightCurveElement> getChildren(HeightCurveElement heightCurveTreeNode) {
        try {
            var nodeChildrenAccessor = HeightCurveElement.class.getDeclaredField("children");
            nodeChildrenAccessor.setAccessible(true);
            return (List<HeightCurveElement>) nodeChildrenAccessor.get(heightCurveTreeNode);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
            throw new TestAbortedException("Could not get children from Height Curve Tree Node", e);
        }
    }
}