package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class HeightCurveTree {
    private final HeightCurveTreeNode root = new HeightCurveTreeNode(
            new HeightCurveElement(
                    new double[] {
                            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, // BL SW
                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, // TL NW
                            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, // TR NE
                            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,  // BR SE
                            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, // BL SW => To close itself
                    },
                    0,
                    Double.POSITIVE_INFINITY,
                    new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}
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

    public List<HeightCurveElement> searchScaled(BoundingBox boundingBox, double minBoundingBoxArea) {
        Collection<HeightCurveElement> elementsConcurrent = new ConcurrentLinkedQueue<>();

        searchScaled(root, boundingBox, elementsConcurrent);

        return elementsConcurrent
                .parallelStream()
                .filter(e -> e.getArea() >= minBoundingBoxArea)
                .toList();
    }
    private void searchScaled(HeightCurveTreeNode node, BoundingBox queryBox, Collection<HeightCurveElement> results) {
        // If heightCurveElement intersects => add to list => Run with children
        if (queryBox.intersects(node.getHeightCurveElement().getBounds())) {
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
        return getHeightCurveForPoint(root, lon, lat);

    }
    private HeightCurveElement getHeightCurveForPoint(HeightCurveTreeNode node, double lon, double lat) {
        if (node.getChildren().isEmpty()) {
            return node.getHeightCurveElement();
        } else {
            var childContaining = node.getChildren()
                    .parallelStream()
                    .filter(e -> e.contains(lon, lat))
                    .findFirst();

            if (childContaining.isPresent()) {
                return getHeightCurveForPoint(childContaining.get(), lon, lat);
            } else {
                return node.getHeightCurveElement();
            }
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
            // Try to find first node which contains element
            // Candidates are children which are bigger than element => might contain so sort by biggest first
            var candidateNodes = node.getChildren()
                    .parallelStream()
                    .filter(e -> e.getArea() > heightCurveElement.getArea()) // TODO: This filter is slow
                    .sorted(Comparator.comparing(HeightCurveTreeNode::getArea).reversed())
                    .toList(); // TODO: This toList is slow

            if (candidateNodes.isEmpty()) {
                // No child contains element => try to find children which the element contains
                var newNode = new HeightCurveTreeNode(heightCurveElement);

                var childrenPossiblyContained = node.getChildren()
                        .parallelStream()
                        .filter(e -> e.getArea() < heightCurveElement.getArea())
                        .toList();

                if (!childrenPossiblyContained.isEmpty()) {
                    // Some node children have a smaller area than element => add contained elements to the new node
                    childrenPossiblyContained
                            .parallelStream()
                            .filter(e -> newNode.contains(e.getHeightCurveElement()))
                            .toList()
                            .forEach(child -> {
                                node.getChildren().remove(child);
                                node.getHeightCurveElement().removeInnerPolygon(child.getHeightCurveElement().getCoordinates());
                                newNode.getChildren().add(child);
                                newNode.getHeightCurveElement().addInnerPolygon(child.getHeightCurveElement().getCoordinates());
                            });
                }

                node.getChildren().add(newNode);
                node.getHeightCurveElement().addInnerPolygon(newNode.getHeightCurveElement().getCoordinates());
            } else {
                // Get the biggest child which contains element
                var biggestChildContaining = findBiggestChildContaining(candidateNodes, heightCurveElement);
                if (biggestChildContaining.isPresent()) {
                    put(biggestChildContaining.get(), heightCurveElement);
                } else {
                    // No child found which contains the element => add it to node
                    node.getChildren().add(new HeightCurveTreeNode(heightCurveElement));
                    node.getHeightCurveElement().addInnerPolygon(heightCurveElement.getCoordinates());
                }
            }
        }
    }

    private Optional<HeightCurveTreeNode> findBiggestChildContaining(List<HeightCurveTreeNode> candidateNodes, HeightCurveElement heightCurveElement) {
        HeightCurveTreeNode biggestChildContaining = null;
        for (var child : candidateNodes) {
            if (biggestChildContaining != null) break;
            if (child.contains(heightCurveElement)) biggestChildContaining = child;
        }
        return Optional.ofNullable(biggestChildContaining);
    }

    public void clear() {
        root.getChildren().clear();
    }
}
