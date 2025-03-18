package dk.itu.data.models.parser;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParserOsmRelation extends ParserOsmElement {
    private final double[] bounds = new double[4];
    private Path2D.Double shape = null;
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final List<double[]> outerPolygons = new ArrayList<>();

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
                case INNER -> {
                    switch (element) {
                        case ParserOsmWay osmWay -> {
                            var coordinates = osmWay.getCoordinates();
                            if (!osmWay.isLine()) {
                                // Closes => add as completed polygon
                                if (!isClockwise(coordinates)) {
                                    coordinates = reversePairs(coordinates);
                                }
                                innerPolygons.add(coordinates);
                            } else {
                                setShouldBeDrawn(false);
                                return;
//                                pathsToAdd.add(coordinates);
                            }
                        }
                        case ParserOsmRelation osmRelation -> {
                            var outerCoordinates = osmRelation.getOuterPolygons();
                            for (var coordinates : outerCoordinates) {
                                if (coordinates[0] == coordinates[coordinates.length - 2] && coordinates[1] == coordinates[coordinates.length - 1]) {
                                    // Closes => add as completed polygon
                                    if (!isClockwise(coordinates)) {
                                        coordinates = reversePairs(coordinates);
                                    }
                                    innerPolygons.add(coordinates);
                                } else {
                                    setShouldBeDrawn(false);
                                    return;
//                                    pathsToAdd.add(coordinates);
                                }
                            }

                            var innerCoordinates = osmRelation.getInnerPolygons();
                            for (var coordinates : innerCoordinates) {
                                if (coordinates[0] == coordinates[coordinates.length - 2] && coordinates[1] == coordinates[coordinates.length - 1]) {
                                    // Closes => add as completed polygon
                                    if (isClockwise(coordinates)) {
                                        coordinates = reversePairs(coordinates);
                                    }
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
                                // Closes => add as completed polygon
                                if (isClockwise(coordinates)) {
                                    coordinates = reversePairs(coordinates);
                                }
                                outerPolygons.add(coordinates);
                            } else {
                                pathsToAdd.add(coordinates);
                            }
                        }
                        case ParserOsmRelation osmRelation -> {
                            var outerCoordinates = osmRelation.getOuterPolygons();
                            for (var coordinates : outerCoordinates) {
                                if (coordinates[0] == coordinates[coordinates.length - 2] && coordinates[1] == coordinates[coordinates.length - 1]) {
                                    // Closes => add as completed polygon
                                    if (isClockwise(coordinates)) {
                                        coordinates = reversePairs(coordinates);
                                    }
                                    outerPolygons.add(coordinates);
                                } else {
                                    pathsToAdd.add(coordinates);
                                }
                            }

                            var innerCoordinates = osmRelation.getInnerPolygons();
                            for (var coordinates : innerCoordinates) {
                                if (coordinates[0] == coordinates[coordinates.length - 2] && coordinates[1] == coordinates[coordinates.length - 1]) {
                                    // Closes => add as completed polygon
                                    if (!isClockwise(coordinates)) {
                                        coordinates = reversePairs(coordinates);
                                    }
                                    outerPolygons.add(coordinates);
                                } else {
                                    pathsToAdd.add(coordinates);
                                }
                            }
                        }
                        default -> {}
                    }
                }
            }

            double[] elementBounds = element.getBounds();
            if (elementBounds[0] < minLat) minLat = elementBounds[0];
            if (elementBounds[1] < minLon) minLon = elementBounds[1];
            if (elementBounds[2] > maxLat) maxLat = elementBounds[2];
            if (elementBounds[3] > maxLon) maxLon = elementBounds[3];
        }
        bounds[0] = minLat;
        bounds[1] = minLon;
        bounds[2] = maxLat;
        bounds[3] = maxLon;

        for (int i = 0; i < pathsToAdd.size(); i++) {
            var ii = pathsToAdd.get(i);

            if (ii[0] == ii[ii.length - 2] && ii[1] == ii[ii.length - 1]) {
                // Test if i closes itself
                if (isClockwise(ii)) {
                    ii = reversePairs(ii);
                }
                outerPolygons.add(ii);
                pathsToAdd.remove(i);
                i = -1;
                continue;
            }

            for (int j = 1; j < pathsToAdd.size(); j++) {
                if (i == j) continue;
                var jj = pathsToAdd.get(j);
                if (ii[0] == jj[0] && ii[1] == jj[1]) {
                    // i start - j start => reverse j and add i
                    List<Double> newJj = Arrays.stream(reversePairs(jj)).parallel().boxed().collect(Collectors.toList());
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.addAll(Arrays.stream(ii).parallel().boxed().toList()); // Add i
                    pathsToAdd.set(j, newJj.parallelStream().mapToDouble(Double::doubleValue).toArray());
                    pathsToAdd.remove(i);
                    i = -1;
                    break;
                } else if (ii[0] == jj[jj.length-2] && ii[1] == jj[jj.length-2]) {
                    // i start - j end => add j then i
                    List<Double> newJj = Arrays.stream(reversePairs(jj)).parallel().boxed().collect(Collectors.toList());
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.addAll(Arrays.stream(ii).boxed().toList()); // Add i
                    pathsToAdd.set(j, newJj.parallelStream().mapToDouble(Double::doubleValue).toArray());
                    pathsToAdd.remove(i);
                    i = -1;
                    break;
                } else if (ii[ii.length-2] == jj[0] && ii[ii.length-1] == jj[1]) {
                    // i end - j start => add j to i
                    List<Double> newIi = Arrays.stream(ii).parallel().boxed().collect(Collectors.toList());
                    newIi.removeLast(); // remove last 2 => included in i
                    newIi.removeLast(); // remove last 2 => included in i
                    newIi.addAll(Arrays.stream(jj).parallel().boxed().toList()); // Add i
                    pathsToAdd.set(i, newIi.parallelStream().mapToDouble(Double::doubleValue).toArray());
                    pathsToAdd.remove(j);
                    i = -1;
                    break;
                } else if (ii[ii.length-2] == jj[jj.length-2] && ii[ii.length-1] == jj[jj.length-1]){
                    // i end - j end => reverse j and add it to i
                    List<Double> newJj = Arrays.stream(jj).parallel().boxed().collect(Collectors.toList());
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.removeLast(); // remove last 2 => included in i
                    newJj.addAll(Arrays.stream(reversePairs(ii)).parallel().boxed().toList()); // Add j
                    pathsToAdd.set(j, newJj.parallelStream().mapToDouble(Double::doubleValue).toArray());
                    pathsToAdd.remove(i);
                    i = -1;
                    break;
                }
            }
        }

        if (!pathsToAdd.isEmpty() || outerPolygons.isEmpty()) {
            setShouldBeDrawn(false);
        } else {
            shape = getPath2D();
        }
    }

    @NotNull
    private Path2D.Double getPath2D() {
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (double[] polygon : outerPolygons) {
            p.moveTo(0.56*polygon[0], -polygon[1]);
            for (int i = 2; i < polygon.length; i+=2) {
                p.lineTo(0.56*polygon[i], -polygon[i+1]);
            }
            p.closePath();
        }
        for (double[] polygon : innerPolygons) {
            p.moveTo(0.56*polygon[0], -polygon[1]);
            for (int i = 2; i < polygon.length; i+=2) {
                p.lineTo(0.56*polygon[i], -polygon[i+1]);
            }
            p.closePath();
        }
        return p;
    }

    @Override
    public double getArea() {
        return (bounds[2]-bounds[0]) * (bounds[3]-bounds[1]);
    }

    @Override
    public double[] getBounds() {
        return bounds;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    public List<double[]> getOuterPolygons() {
        return outerPolygons;
    }
    public List<double[]> getInnerPolygons() {
        return innerPolygons;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        g2d.fill(shape);
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

