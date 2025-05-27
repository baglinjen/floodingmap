package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.heightcurve.HeightCurveElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class HeightCurveTree {
    private final HeightCurveTreeNode root = new HeightCurveTreeNode(
            new HeightCurveElement(
                    new double[] {
                            -180, -90, // BL SW
                            180, -90,  // BR SE
                            180, 90, // TR NE
                            -180, 90, // TL NW
                            -180, -90 // BL SW
                    },
                    0,
                    new double[] {-180, -90, 180, 90}
            )
    );
    private float minWaterLevel = 0, maxWaterLevel = 1;

    public float getMaxWaterLevel() {
        return maxWaterLevel;
    }

    public float getMinWaterLevel() {
        return minWaterLevel;
    }

    public List<HeightCurveElement> getElements() {
        Queue<HeightCurveElement> elements = new ConcurrentLinkedQueue<>();
        getElements(root, elements);
        return elements.parallelStream().toList();
    }
    private void getElements(HeightCurveTreeNode node, Collection<HeightCurveElement> elements) {
        elements.add(node.getHeightCurveElement());
        node.getChildren().parallelStream().forEach(child -> getElements(child, elements));
    }

    public List<HeightCurveElement> searchScaled(double[] boundingBox, double minBoundingBoxArea) {
        Collection<HeightCurveElement> elementsConcurrent = new ConcurrentLinkedQueue<>();

        searchScaled(root, boundingBox, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> e.getArea() >= minBoundingBoxArea)
                .toList();
    }
    private void searchScaled(HeightCurveTreeNode node, double[] queryBox, Collection<HeightCurveElement> results) {
        // If heightCurveElement intersects => add to list => Run with children
        if (node.getHeightCurveElement().intersects(queryBox)) {
            results.add(node.getHeightCurveElement());
            node.getChildren().parallelStream().forEach(child -> searchScaled(child, queryBox, results));
        }
        // Else => nothing
    }

    public List<List<HeightCurveElement>> getFloodingSteps(float waterLevel) {
        if (waterLevel <= 0) {
            // Returns empty list
            return new ArrayList<>();
        }

        Map<Integer, Collection<HeightCurveElement>> steps = new ConcurrentHashMap<>();
        int stepDepth = 0;

        getFloodingSteps(root, steps, waterLevel, stepDepth);

        return IntStream.range(0, steps.size()).mapToObj(i -> steps.get(i).stream().toList()).toList();
    }
    private void getFloodingSteps(HeightCurveTreeNode node, Map<Integer, Collection<HeightCurveElement>> steps, float waterLevel, int stepDepth) {
        if (node.getHeightCurveElement().getHeight() <= waterLevel) {
            steps.putIfAbsent(stepDepth, new ConcurrentLinkedQueue<>());
            steps.get(stepDepth).add(node.getHeightCurveElement());

            // Add elements lower than node => they're now flooded
            node.getChildren()
                    .parallelStream()
                    .filter(child -> child.getHeightCurveElement().getHeight() <= node.getHeightCurveElement().getHeight())
                    .forEach(child -> getElements(child, steps.get(stepDepth)));

            // Continue iteration through other nodes
            node.getChildren()
                    .parallelStream()
                    .filter(child -> child.getHeightCurveElement().getHeight() > node.getHeightCurveElement().getHeight())
                    .forEach(c -> getFloodingSteps(c, steps, waterLevel, stepDepth+1));
        }
    }

    public HeightCurveElement getHeightCurveForPoint(double lon, double lat) {
        return getHeightCurveForPoint(root, lon, lat).orElse(root.getHeightCurveElement());
    }

    private Optional<HeightCurveElement> getHeightCurveForPoint(HeightCurveTreeNode node, double lon, double lat) {
        if (node.getChildren().isEmpty()) {
            // Return the height curve if it doesn't have any children
            return Optional.of(node.getHeightCurveElement());
        } else {
            // Iterate through each child and find the first one containing
            Optional<HeightCurveTreeNode> childContaining = Optional.empty();
            for (int i = 0; i < node.getChildren().size() && childContaining.isEmpty(); i++) {
                if (node.getChildren().get(i).contains(lon, lat)) {
                    childContaining = Optional.of(node.getChildren().get(i));
                }
            }
            return childContaining.map(child -> getHeightCurveForPoint(child, lon, lat).orElse(child.getHeightCurveElement()));
        }
    }

    public void put(HeightCurveElement heightCurveElement) {
        minWaterLevel = Math.max(Math.min(minWaterLevel, heightCurveElement.getHeight()), 0);
        maxWaterLevel = Math.max(maxWaterLevel, heightCurveElement.getHeight());
        put(root, heightCurveElement);
    }
    private void put(HeightCurveTreeNode node, HeightCurveElement heightCurveElement) {
        if (node.getChildren().isEmpty()) {
            // Node has no children => add element
            node.getChildren().add(new HeightCurveTreeNode(heightCurveElement));
            node.getHeightCurveElement().addInnerPolygon(heightCurveElement.getCoordinates());
        } else {
            // Try to find first node which contains element and add => otherwise add to current node
            node.getChildren()
                    .parallelStream()
                    .filter(e -> e.getHeightCurveElement().contains(heightCurveElement))
                    .findFirst()
                    .ifPresentOrElse(
                            containingNode -> put(containingNode, heightCurveElement),
                            () -> {
                                // No child contains element => try to find children which the element contains
                                var newNode = new HeightCurveTreeNode(heightCurveElement);
                                node.getChildren()
                                        .parallelStream()
                                        .filter(e -> heightCurveElement.contains(e.getHeightCurveElement()))
                                        .toList()
                                        .forEach(child -> {
                                            node.getChildren().remove(child);
                                            node.getHeightCurveElement().removeInnerPolygon(child.getHeightCurveElement().getCoordinates());
                                            newNode.getChildren().add(child);
                                            newNode.getHeightCurveElement().addInnerPolygon(child.getHeightCurveElement().getCoordinates());
                                        });

                                node.getChildren().add(newNode);
                                node.getHeightCurveElement().addInnerPolygon(newNode.getHeightCurveElement().getCoordinates());
                            }
                    );
        }
    }

    public void clear() {
        root.getChildren().clear();
        root.getHeightCurveElement().removeAllInnerPolygons();
    }
}