package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import static dk.itu.util.PolygonUtils.contains;
import static dk.itu.util.PolygonUtils.isPointInPolygon;

public class HeightCurveTree {
    private final Logger logger = LoggerFactory.getLogger();
    private final HeightCurveTreeNode root = new HeightCurveTreeNode(
            HeightCurveElement.mapToHeightCurveElement(
                    new ParserHeightCurveElement(
                            0,
                            new double[] {
                                    -180, -90, // BL SW
                                    -180, 90, // TL NW
                                    180, 90, // TR NE
                                    180, -90  // BR SE
                            },
                            0
                    )
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
            node.children
                    .parallelStream()
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
                    .filter(e -> isPointInPolygon(e.heightCurveElement.getCoordinates(), lon, lat))
                    .findFirst();

            if (childContaining.isPresent()) {
                return getHeightCurveForPoint(childContaining.get(), lon, lat);
            } else {
                return node.heightCurveElement;
            }
        }
    }

    public void put(ParserHeightCurveElement heightCurveElement) {
        minWaterLevel = Math.min(minWaterLevel, heightCurveElement.getHeight());
        maxWaterLevel = Math.max(maxWaterLevel, heightCurveElement.getHeight());
        put(root, HeightCurveElement.mapToHeightCurveElement(heightCurveElement));
    }
    private void put(HeightCurveTreeNode node, HeightCurveElement heightCurveElement) {
        if (node.children.isEmpty()) {
            // Root has no children => add element
            node.children.add(new HeightCurveTreeNode(heightCurveElement));
            node.heightCurveElement.addInnerPolygon(heightCurveElement.getCoordinates());
        } else {
            // Try to find first node which contains element
            var candidateNodes = node.children
                    .parallelStream()
                    .filter(e -> e.heightCurveElement.getArea() > heightCurveElement.getArea() && contains(e.heightCurveElement.getCoordinates(), heightCurveElement.getCoordinates()))
                    .toList();

            if (candidateNodes.isEmpty()) {
                // No child contains element => try to find children which element contains
                var newNode = new HeightCurveTreeNode(heightCurveElement);

                node.children
                        .parallelStream()
                        .filter(e -> e.heightCurveElement.getArea() < heightCurveElement.getArea() && contains(heightCurveElement.getCoordinates(), e.heightCurveElement.getCoordinates()))
                        .toList()
                        .forEach(child -> {
                            node.children.remove(child);
                            node.heightCurveElement.removeInnerPolygon(child.heightCurveElement.getCoordinates());
                            newNode.children.add(child);
                            newNode.heightCurveElement.addInnerPolygon(child.heightCurveElement.getCoordinates());
                        });

                node.children.add(newNode);
                node.heightCurveElement.addInnerPolygon(newNode.heightCurveElement.getCoordinates());
            } else {
                // Found parent for element
                if (candidateNodes.size() > 1) { logger.error("Found more than one candidate node for height curve tree"); }
                put(candidateNodes.getFirst(), heightCurveElement);
            }
        }
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
    }
}
