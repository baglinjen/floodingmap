package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.PolygonUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class HeightCurveTree {
    private static final int VERTICES_FOR_CONCURRENCY = 1000;
    private final HeightCurveTreeNode root = new HeightCurveTreeNode(
            new HeightCurveElement(
                    new double[] {
                            -180, -90, // BL SW
                            -180, 90, // TL NW
                            180, 90, // TR NE
                            180, -90  // BR SE
                    },
                    0,
                    Double.MAX_VALUE,
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
    private void getElements(HeightCurveTreeNode node, Queue<HeightCurveElement> elements) {
        elements.add(node.heightCurveElement);
        node.children.parallelStream().forEach(child -> getElements(child, elements));
    }

    public List<List<HeightCurveElement>> getFloodingStepsConcurrent(float waterLevel) {
        if (waterLevel <= 0) {
            // Returns empty list
            return new ArrayList<>();
        }

        Map<Integer, Queue<HeightCurveElement>> steps = new ConcurrentHashMap<>();
        int stepDepth = 0;

        steps.put(stepDepth, new ConcurrentLinkedQueue<>());
        steps.get(stepDepth).add(root.heightCurveElement);

        getFloodingStepsConcurrent(root, steps, waterLevel, stepDepth+1);

        return IntStream.range(0, steps.size()).mapToObj(i -> steps.get(i).stream().toList()).toList();
    }
    private void getFloodingStepsConcurrent(HeightCurveTreeNode node, Map<Integer, Queue<HeightCurveElement>> steps, float waterLevel, int stepDepth) {
        if (node.heightCurveElement.getHeight() <= waterLevel) {
            steps.putIfAbsent(stepDepth, new ConcurrentLinkedQueue<>());
            steps.get(stepDepth).add(node.heightCurveElement);

            // Add elements lower than node => they're now flooded
            node.children
                    .parallelStream()
                    .filter(child -> child.heightCurveElement.getHeight() <= node.heightCurveElement.getHeight())
                    .forEach(child -> getElements(child, steps.get(stepDepth)));

            // Continue iteration through other nodes
            node.children
                    .parallelStream()
                    .filter(child -> child.heightCurveElement.getHeight() > node.heightCurveElement.getHeight())
                    .forEach(c -> getFloodingStepsConcurrent(c, steps, waterLevel, stepDepth+1));
        }
    }

    public HeightCurveElement getHeightCurveForPoint(double lon, double lat) {
        return getHeightCurveForPoint(root, lon, lat);

    }
    private HeightCurveElement getHeightCurveForPoint(HeightCurveTreeNode node, double lon, double lat) {
        if (node.children.isEmpty()) {
            return node.heightCurveElement;
        } else {
            var childContaining = node
                    .children
                    .parallelStream()
                    .filter(e -> e.contains(lon, lat))
                    .findFirst();

            if (childContaining.isPresent()) {
                return getHeightCurveForPoint(childContaining.get(), lon, lat);
            } else {
                return node.heightCurveElement;
            }
        }
    }

    public void put(ParserHeightCurveElement heightCurveElement) {
        minWaterLevel = Math.max(Math.min(minWaterLevel, heightCurveElement.getHeight()), 0);
        maxWaterLevel = Math.max(maxWaterLevel, heightCurveElement.getHeight());
        put(root, HeightCurveElement.mapToHeightCurveElement(heightCurveElement));
    }
    private void put(HeightCurveTreeNode node, HeightCurveElement heightCurveElement) {
        if (node.children.isEmpty()) {
            // Node has no children => add element
            node.children.add(new HeightCurveTreeNode(heightCurveElement));
            node.heightCurveElement.addInnerPolygon(heightCurveElement.getCoordinates());
        } else {
            // Try to find first node which contains element
            // Candidates are children which are bigger than element => might contain so sort by biggest first
            var candidateNodes = node.children
                    .parallelStream()
                    .filter(e -> e.getArea() > heightCurveElement.getArea())
                    .sorted(Comparator.comparing(HeightCurveTreeNode::getArea).reversed())
                    .toList();

            if (candidateNodes.isEmpty()) {
                // No child contains element => try to find children which the element contains
                var newNode = new HeightCurveTreeNode(heightCurveElement);

                var childrenPossiblyContained = node.children
                        .parallelStream()
                        .filter(e -> e.getArea() < heightCurveElement.getArea())
                        .toList();

                if (!childrenPossiblyContained.isEmpty()) {
                    // Some node children have a smaller area than element => add contained elements to the new node
                    childrenPossiblyContained
                            .parallelStream()
                            .filter(e -> newNode.contains(e.heightCurveElement))
                            .toList()
                            .forEach(child -> {
                                node.children.remove(child);
                                node.heightCurveElement.removeInnerPolygon(child.heightCurveElement.getCoordinates());
                                newNode.children.add(child);
                                newNode.heightCurveElement.addInnerPolygon(child.heightCurveElement.getCoordinates());
                            });
                }

                node.children.add(newNode);
                node.heightCurveElement.addInnerPolygon(newNode.heightCurveElement.getCoordinates());
            } else {
                // Get the biggest child which contains element
                var shouldUseConcurrent = (
                                candidateNodes
                                        .parallelStream()
                                        .map(e -> e.heightCurveElement.getCoordinates().length)
                                        .reduce(0, Integer::sum) / 2
                                +
                                        heightCurveElement.getCoordinates().length / 2
                        ) > VERTICES_FOR_CONCURRENCY;

                var biggestChildContaining = shouldUseConcurrent ?
                        findBiggestChildContainingConcurrent(candidateNodes, heightCurveElement)
                        :
                        findBiggestChildContainingBlocking(candidateNodes, heightCurveElement);

                if (biggestChildContaining.isPresent()) {
                    put(biggestChildContaining.get(), heightCurveElement);
                } else {
                    // No child found which contains the element => add it to node
                    node.children.add(new HeightCurveTreeNode(heightCurveElement));
                    node.heightCurveElement.addInnerPolygon(heightCurveElement.getCoordinates());
                }
            }
        }
    }

    private Optional<HeightCurveTreeNode> findBiggestChildContainingBlocking(List<HeightCurveTreeNode> candidateNodes, HeightCurveElement heightCurveElement) {
        HeightCurveTreeNode biggestChildContaining = null;
        for (var child : candidateNodes) {
            if (biggestChildContaining != null) break;
            if (child.contains(heightCurveElement)) biggestChildContaining = child;
        }
        return Optional.ofNullable(biggestChildContaining);
    }
    private Optional<HeightCurveTreeNode> findBiggestChildContainingConcurrent(List<HeightCurveTreeNode> candidateNodes, HeightCurveElement heightCurveElement) {
        final HeightCurveTreeNode[] bn = {null};
        IntStream.range(0, candidateNodes.size())
                .parallel()
                .forEach(i -> {
                    var c = candidateNodes.get(i);
                    if (bn[0] == null) {
                        if (c.contains(heightCurveElement)) bn[0] = c;
                    }
                });
        return Optional.ofNullable(bn[0]);
    }

    private Optional<HeightCurveTreeNode> findC(List<HeightCurveTreeNode> candidateNodes, HeightCurveElement heightCurveElement) {
        AtomicReference<HeightCurveTreeNode> bn4 = new AtomicReference<>(null);
        IntStream.range(0, candidateNodes.size())
                .parallel()
                .forEach(i -> {
                    if (bn4.get() == null) {
                        if (candidateNodes.get(i).contains(heightCurveElement)) {
                            bn4.set(candidateNodes.get(i));
                        }
                    }
                });
        return Optional.ofNullable(bn4.get());
    }

    public void clear() {
        root.children.clear();
    }

    private static class HeightCurveTreeNode {
        private final HeightCurveElement heightCurveElement;
        private final List<HeightCurveTreeNode> children;

        private HeightCurveTreeNode(HeightCurveElement heightCurveElement) {
            this.heightCurveElement = heightCurveElement;
            this.children = new ArrayList<>();
        }

        public double getArea() {
            return heightCurveElement.getArea();
        }

        public boolean contains(HeightCurveElement element) {
            var containsBbox =
                    element.getBounds()[0] >= heightCurveElement.getBounds()[0] &&
                    element.getBounds()[1] >= heightCurveElement.getBounds()[1] &&
                    element.getBounds()[2] <= heightCurveElement.getBounds()[2] &&
                    element.getBounds()[3] <= heightCurveElement.getBounds()[3];

            if (containsBbox) {
                return PolygonUtils.contains(this.heightCurveElement.getCoordinates(), element.getCoordinates());
            } else {
                return false;
            }
        }

        public boolean contains(double lon, double lat) {
            var containsBbox =
                    lon >= heightCurveElement.getBounds()[0] &&
                    lat >= heightCurveElement.getBounds()[1] &&
                    lon <= heightCurveElement.getBounds()[2] &&
                    lat <= heightCurveElement.getBounds()[3];

            if (containsBbox) {
                return PolygonUtils.isPointInPolygon(this.heightCurveElement.getCoordinates(), lon, lat);
            } else {
                return false;
            }
        }
    }
}
