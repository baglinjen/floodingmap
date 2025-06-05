package dk.itu.data.datastructure.heightcurvetree;

import dk.itu.data.models.heightcurve.HeightCurveElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class HeightCurveTree {
    private final HeightCurveElement root = new HeightCurveElement(
                    new float[] {
                            -180, -90, // BL SW
                            180, -90,  // BR SE
                            180, 90, // TR NE
                            -180, 90, // TL NW
                            -180, -90 // BL SW
                    },
                    0
            );
    private float minWaterLevel = 0, maxWaterLevel = 1;

    public float getMaxWaterLevel() {
        return maxWaterLevel;
    }

    public float getMinWaterLevel() {
        return minWaterLevel;
    }

    public void getElements(List<HeightCurveElement> heightCurves) {
        getElements(root, heightCurves);
    }
    private void getElements(HeightCurveElement node, Collection<HeightCurveElement> elements) {
        elements.add(node);
        for (HeightCurveElement child : node.getChildren()) {
            getElements(child, elements);
        }
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
    private void getFloodingSteps(HeightCurveElement node, Map<Integer, Collection<HeightCurveElement>> steps, float waterLevel, int stepDepth) {
        if (node.getHeight() <= waterLevel) {
            steps.putIfAbsent(stepDepth, new ConcurrentLinkedQueue<>());
            steps.get(stepDepth).add(node);

            // Add elements lower than node => they're now flooded
            node.getChildren()
                    .parallelStream()
                    .filter(child -> child.getHeight() <= node.getHeight())
                    .forEach(child -> getElements(child, steps.get(stepDepth)));

            // Continue iteration through other nodes
            node.getChildren()
                    .parallelStream()
                    .filter(child -> child.getHeight() > node.getHeight())
                    .forEach(c -> getFloodingSteps(c, steps, waterLevel, stepDepth+1));
        }
    }

    public HeightCurveElement getHeightCurveForPoint(float lon, float lat) {
        var x = getHeightCurveForPoint(root, lon, lat).orElse(root);
        return x;
    }

    private Optional<HeightCurveElement> getHeightCurveForPoint(HeightCurveElement node, float lon, float lat) {
        if (node.getChildren().isEmpty()) {
            // Return the height curve if it doesn't have any children
            return Optional.of(node);
        } else {
            // Iterate through each child and find the first one containing
            Optional<HeightCurveElement> childContaining = Optional.empty();
            for (int i = 0; i < node.getChildren().size() && childContaining.isEmpty(); i++) {
                if (node.getChildren().get(i).containsPoint(lon, lat)) {
                    childContaining = Optional.of(node.getChildren().get(i));
                }
            }
            return childContaining.map(child -> getHeightCurveForPoint(child, lon, lat).orElse(child));
        }
    }

    public void put(HeightCurveElement heightCurveElement) {
        minWaterLevel = Math.max(Math.min(minWaterLevel, heightCurveElement.getHeight()), 0);
        maxWaterLevel = Math.max(maxWaterLevel, heightCurveElement.getHeight());

        boolean badlyDrawn =
                heightCurveElement.getHeight() == 2.5f &&
                heightCurveElement.getCoordinates().length == 12578;

        if (badlyDrawn) {
            System.out.println("Badly drawn");
        }

        put(root, heightCurveElement);
    }
    private void put(HeightCurveElement node, HeightCurveElement heightCurveElement) {
        if (node.getChildren().isEmpty()) {
            // Node has no children => add element
            node.addChild(heightCurveElement);
        } else {
            // Try to find first node which contains element and add => otherwise add to current node
            node.getChildren()
                    .parallelStream()
                    .filter(e -> e.contains(heightCurveElement))
                    .findFirst()
                    .ifPresentOrElse(
                            containingNode -> put(containingNode, heightCurveElement),
                            () -> {
                                // No child contains element => try to find children which the element contains
                                node.getChildren()
                                        .parallelStream()
                                        .filter(heightCurveElement::contains)
                                        .toList()
                                        .forEach(child -> {
                                            node.removeChild(child);
                                            heightCurveElement.addChild(child);
                                        });

                                node.addChild(heightCurveElement);
                            }
                    );
        }
    }

    public void clear() {
        root.getChildren().clear();
        root.removeAllInnerPolygons();
    }
}