package dk.itu.common.models.osm;

import kotlin.Pair;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmRelation extends OsmElement {
    private final double[] bounds = new double[4];
    private Shape shape = null;

    public OsmRelation(long id, List<Pair<OsmElement, OsmRelationMemberType>> elements, OsmRelationType type) {
        super(id);
        if (type != OsmRelationType.MULTIPOLYGON) {
            setShouldBeDrawn(false);
            return;
        }


        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE, maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;
        List<OsmElement> outerAreas = new ArrayList<>();
        List<OsmElement> outerPaths = new ArrayList<>();
        List<OsmElement> innerElements = new ArrayList<>();

        for (var pair : elements) {
            var element = pair.getFirst();
            var memberType = pair.getSecond();

            element.setShouldBeDrawn(false);

            switch (memberType) {
                case INNER -> innerElements.add(element);
                case OUTER -> {
                    switch (element.getShape()) {
                        case Area _ -> outerAreas.add(element);
                        case Path2D _ -> outerPaths.add(element);
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

        // Chain all outer paths
        Path2D path = null;
        if (!outerPaths.isEmpty()) {
            path = new Path2D.Double();
            // TODO: Fix edge cases in chaining (fx counter-clockwise ways with clockwise nodes)
            for (OsmElement outerPath : outerPaths) {
                path.append(outerPath.getShape(), true);
            }
        }

        Area area = null;
        if (path != null) {
            area = new Area(path);
        }

        // Add outer paths to outer areas if any
        if (!outerAreas.isEmpty()) {
            if (area == null) area = new Area();
            for (var element : outerAreas) {
                area.add(new Area(element.getShape()));
            }
        }
        // Subtract inner paths if any
        if (!innerElements.isEmpty() && area != null) {
            for (var element : innerElements) {
                area.subtract(new Area(element.getShape()));
            }
        }

        if (area == null) {
            setShouldBeDrawn(false);
        } else {
            shape = area;
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

    @Override
    public Shape getShape() {
        return shape;
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
