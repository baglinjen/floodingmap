package dk.itu.data.models.parser;

import kotlin.Pair;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;

public class ParserOsmRelation extends ParserOsmElement {
    private final double[] bounds = new double[4];
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final List<double[]> outerPolygons = new ArrayList<>();
    private final Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);

    public ParserOsmRelation(long id, List<Pair<ParserOsmElement, OsmRelationMemberType>> elements, OsmRelationType type) {
        super(id);

        if (type != OsmRelationType.MULTIPOLYGON) {
            setShouldBeDrawn(false);
            return;
        }

        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE, maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;

        List<double[]> openElements = new ArrayList<>();

        // Sort elements
        for (var elementPair : elements) {
            var element = elementPair.getFirst();
            var elementType = elementPair.getSecond();

            // Disable drawing since it is now part of the relation drawing
            element.setShouldBeDrawn(false);

            // Find bounds
            double[] elementBounds = element.getBounds();
            if (elementBounds[0] < minLon) minLon = elementBounds[0];
            if (elementBounds[1] < minLat) minLat = elementBounds[1];
            if (elementBounds[2] > maxLon) maxLon = elementBounds[2];
            if (elementBounds[3] > maxLat) maxLat = elementBounds[3];

            switch (element) {
                case ParserOsmWay osmWay -> {
                    var coordinates = osmWay.getCoordinates();
                    if (elementType == OsmRelationMemberType.INNER) {
                        innerPolygons.add(coordinates);
                    } else {
                        // Outer that should be closed
                        if (isClosed(coordinates)) {
                            outerPolygons.add(coordinates);
                        } else {
                            openElements.add(coordinates);
                        }
                    }
                }
                case ParserOsmRelation osmRelation -> {
                    if (elementType == OsmRelationMemberType.INNER) {
                        innerPolygons.addAll(osmRelation.getOuterPolygons());
                    } else {
                        for (double[] polygon : osmRelation.getOuterPolygons()) {
                            if (isClosed(polygon)) {
                                outerPolygons.add(polygon);
                            } else {
                                openElements.add(polygon);
                            }
                        }
                    }
                }
                default -> {}
            }
        }

        // Set bounds
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        // Stitch open elements
        for (int i = 0; i < openElements.size(); i++) {
            // Iterate i trying to close j
            var polygonI = openElements.get(i);
            // If i is closed already => add to closed and i-- && continue
            if (isClosed(polygonI)) {
                openElements.remove(polygonI);
                outerPolygons.add(polygonI);
                i--;
                continue;
            }

            for (int j = i + 1; j < openElements.size(); j++) {
                var polygonJ = openElements.get(j);

                // If i is added to j => skip through j continue with i (j = unclosedElements.size & i--)
                switch (findOpenPolygonMatchType(polygonI, polygonJ)) {
                    case FIRST_FIRST -> {
                        // Remove i to add to j
                        openElements.remove(polygonI);

                        // Add i to j
                        var newPolygonJ = appendExcludingN(reversePairs(polygonI), polygonJ, 2);
                        openElements.remove(polygonJ);

                        // Add j to closed if it is now closed
                        if (isClosed(newPolygonJ)) {
                            outerPolygons.add(forceCounterClockwise(newPolygonJ));
                        } else {
                            openElements.add(newPolygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = openElements.size();
                        i--;
                    }
                    case LAST_FIRST -> {
                        // Remove i to add to j
                        openElements.remove(polygonI);

                        // Add i to j
                        var newPolygonJ = appendExcludingN(polygonI, polygonJ, 2);
                        openElements.remove(polygonJ);

                        // Add j to closed if it is now closed
                        if (isClosed(newPolygonJ)) {
                            outerPolygons.add(forceCounterClockwise(newPolygonJ));
                        } else {
                            openElements.add(newPolygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = openElements.size();
                        i--;
                    }
                    case FIRST_LAST -> {
                        // Remove i to add to j
                        openElements.remove(polygonI);

                        // Add i to j
                        var newPolygonJ = appendExcludingN(polygonJ, polygonI, 2);
                        openElements.remove(polygonJ);

                        // Add j to closed if it is now closed
                        if (isClosed(newPolygonJ)) {
                            outerPolygons.add(forceCounterClockwise(newPolygonJ));
                        } else {
                            openElements.add(newPolygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = openElements.size();
                        i--;
                    }
                    case LAST_LAST -> {
                        // Remove i to add to j
                        openElements.remove(polygonI);

                        // Add i to j
                        var newPolygonJ = appendExcludingN(polygonJ, reversePairs(polygonI), 2);
                        openElements.remove(polygonJ);

                        // Add j to closed if it is now closed
                        if (isClosed(newPolygonJ)) {
                            outerPolygons.add(forceCounterClockwise(newPolygonJ));
                        } else {
                            openElements.add(newPolygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = openElements.size();
                        i--;
                    }
                }
            }
        }

        if (!openElements.isEmpty() || outerPolygons.isEmpty()) {
            setShouldBeDrawn(false);
        }
    }

    @Override
    public double getArea() {
        return (bounds[2]-bounds[0]) * (bounds[3]-bounds[1]);
    }

    @Override
    public double[] getBounds() {
        return bounds;
    }

    public List<double[]> getOuterPolygons() {
        return outerPolygons;
    }
    public List<double[]> getInnerPolygons() {
        return innerPolygons;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        // No need to draw
    }

    public enum OsmRelationType {
        MULTIPOLYGON,
        ROUTE;

        public static OsmRelationType fromTags(Map<String, String> tags) {
            return switch (tags.get("type")) {
                case "multipolygon" -> MULTIPOLYGON;
                case "route" -> ROUTE;
                default -> null;
            };
        }
    }

    public enum OsmRelationMemberType {
        INNER,
        OUTER;

        public static OsmRelationMemberType fromString(String type) {
            return switch (type) {
                case "inner" -> INNER;
                case "outer" -> OUTER;
                default -> null;
            };
        }
    }
}