package dk.itu.data.models.parser;

import dk.itu.util.shape.RelationPath;
import kotlin.Pair;

import java.util.*;
import java.util.List;

import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;

public class ParserOsmRelation implements ParserOsmElement {
    private final long id;
    private byte styleId;
    private RelationPath relationPath;

    public ParserOsmRelation(long id, List<Pair<ParserOsmElement, OsmRelationMemberType>> elements, OsmRelationType type, byte styleId) {
        this.id = id;
        this.styleId = styleId;
        if (type != OsmRelationType.MULTIPOLYGON) {
            setStyleId((byte) -1);
            return;
        }

        List<double[]> outerPolygons = new ArrayList<>();
        List<double[]> innerPolygons = new ArrayList<>();
        List<double[]> openElements = new ArrayList<>();

        // Sort elements
        for (var elementPair : elements) {
            var element = elementPair.getFirst();
            var elementType = elementPair.getSecond();

            // Disable drawing since it is now part of the relation drawing
            element.setStyleId((byte) -1);

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
            setStyleId((byte) -1);
        } else {
            relationPath = new RelationPath(outerPolygons, innerPolygons);
        }
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setStyleId(byte styleId) {
        this.styleId = styleId;
    }

    @Override
    public byte getStyleId() {
        return this.styleId;
    }

    @Override
    public boolean shouldBeDrawn() {
        return this.styleId >= 0;
    }

    public RelationPath getPath() {
        return relationPath;
    }

    public List<double[]> getOuterPolygons() {
        if (relationPath == null) return List.of();
        return relationPath.getOuterPolygons();
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