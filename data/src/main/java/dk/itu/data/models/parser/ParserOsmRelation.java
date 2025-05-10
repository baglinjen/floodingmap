package dk.itu.data.models.parser;

import dk.itu.util.LoggerFactory;
import kotlin.Pair;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;
import static dk.itu.util.ShapePreparer.*;

public class ParserOsmRelation extends ParserOsmElement {
    private static final Logger logger = LoggerFactory.getLogger();
    private final double[] bounds = new double[4];
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final List<double[]> outerPolygons = new ArrayList<>();
    private Path2D.Double path = null;

    public ParserOsmRelation(long id, List<Pair<ParserOsmElement, OsmRelationMemberType>> elements, OsmRelationType type) {
        super(id);

        if (type != OsmRelationType.MULTIPOLYGON) {
            setShouldBeDrawn(false);
            return;
        }

        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE, maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;
        List<double[]> pathsToAdd = new ArrayList<>();

        // Calculate raw coordinates
        for (var pair : elements) {
            var element = pair.getFirst();
            var memberType = pair.getSecond();

            element.setShouldBeDrawn(false);

            switch (memberType) {
                case null -> logger.warn("Relation with id {} has invalid member", id);
                case INNER -> {
                    switch (element) {
                        case ParserOsmWay osmWay -> {
                            var coordinates = osmWay.getCoordinates();
                            if (!osmWay.isLine()) {
                                // Closes => add as completed polygon => ensure inners are counter clockwise
                                coordinates = forceCounterClockwise(coordinates);
                                innerPolygons.add(coordinates);
                            } else {
                                setShouldBeDrawn(false);
                                return;
                            }
                        }
                        case ParserOsmRelation osmRelation -> {
                            var outerCoordinates = osmRelation.getOuterPolygons();
                            for (var coordinates : outerCoordinates) {
                                if (isClosed(coordinates)) {
                                    // Closes => add as completed polygon => ensure inners are counter clockwise
                                    coordinates = forceCounterClockwise(coordinates);
                                    innerPolygons.add(coordinates);
                                } else {
                                    setShouldBeDrawn(false);
                                    return;
                                }
                            }

                            var innerCoordinates = osmRelation.getInnerPolygons();
                            for (var coordinates : innerCoordinates) {
                                if (isClosed(coordinates)) {
                                    // Closes => add as completed polygon => ensure inner-inners are clockwise
                                    coordinates = forceClockwise(coordinates);
                                    innerPolygons.add(coordinates);
                                } else {
                                    pathsToAdd.add(coordinates);
                                }
                            }
                        }
                        default -> {}
                    }
                }
                case OUTER -> {
                    switch (element) {
                        case ParserOsmWay osmWay -> {
                            var coordinates = osmWay.getCoordinates();
                            if (!osmWay.isLine()) {
                                // Closes => add as completed polygon => ensure outers are clockwise
                                coordinates = forceClockwise(coordinates);
                                outerPolygons.add(coordinates);
                            } else {
                                pathsToAdd.add(coordinates);
                            }
                        }
                        case ParserOsmRelation osmRelation -> {
                            var outerCoordinates = osmRelation.getOuterPolygons();
                            for (var coordinates : outerCoordinates) {
                                if (isClosed(coordinates)) {
                                    // Closes => add as completed polygon => ensure outers are clockwise
                                    coordinates = forceClockwise(coordinates);
                                    outerPolygons.add(coordinates);
                                } else {
                                    pathsToAdd.add(coordinates);
                                }
                            }

                            var innerCoordinates = osmRelation.getInnerPolygons();
                            for (var coordinates : innerCoordinates) {
                                if (isClosed(coordinates)) {
                                    // Closes => add as completed polygon => ensure outer-inners are counter clockwise
                                    coordinates = forceCounterClockwise(coordinates);
                                    outerPolygons.add(coordinates);
                                } else {
                                    pathsToAdd.add(coordinates);
                                }
                            }
                        }
                        default -> {}
                    }
                }
                default -> {}
            }

            double[] elementBounds = element.getBounds();
            if (elementBounds[0] < minLon) minLon = elementBounds[0];
            if (elementBounds[1] < minLat) minLat = elementBounds[1];
            if (elementBounds[2] > maxLon) maxLon = elementBounds[2];
            if (elementBounds[3] > maxLat) maxLat = elementBounds[3];
        }
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        for (int i = 0; i < pathsToAdd.size(); i++) {
            var ii = pathsToAdd.get(i);

            if (isClosed(ii)) {
                // Test if i closes itself => ensure it is counterclockwise as outer
                ii = forceCounterClockwise(ii);
                outerPolygons.add(ii);
                pathsToAdd.remove(i);
                i = -1;
                continue;
            }

            for (int j = 0; j < pathsToAdd.size(); j++) {
                if (i == j || i == -1) continue;
                var jj = pathsToAdd.get(j);

                switch (findOpenPolygonMatchType(ii, jj)) {
                    case FIRST_FIRST -> {
                        // i start - j start => reverse j and add i
                        pathsToAdd.set(j, appendExcludingN(reversePairs(jj), ii, 2));
                        pathsToAdd.remove(i);
                        i = -1;
                    }
                    case FIRST_LAST -> {
                        // i start - j end => add j then i
                        pathsToAdd.set(j, appendExcludingN(jj, ii, 2));
                        pathsToAdd.remove(i);
                        i = -1;
                    }
                    case LAST_FIRST -> {
                        // i end - j start => add j to i
                        pathsToAdd.set(j, appendExcludingN(ii, jj, 2));
                        pathsToAdd.remove(i);
                        i = -1;
                    }
                    case LAST_LAST -> {
                        // i end - j end => reverse j and add it to i
                        pathsToAdd.set(j, appendExcludingN(ii, reversePairs(jj), 2));
                        pathsToAdd.remove(i);
                        i = -1;
                    }
                }
            }
        }

        if (!pathsToAdd.isEmpty() || outerPolygons.isEmpty()) {
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
    public void prepareDrawing(Graphics2D g2d) {
        path = prepareComplexPolygon(g2d, outerPolygons, innerPolygons, 1);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (path == null) return;

        g2d.setColor(getColor());
        g2d.fill(path);
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

