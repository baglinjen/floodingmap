package dk.itu.util;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

public class ShapePreparer {
    public static void prepareLinePath(Path2D.Double shape, Graphics2D g2d, double[] line, double tolerance) {
        shape.reset();
        if (calculateShape(shape, g2d, line, tolerance) < 3) {
            shape.reset();
        }
    }

    public static void preparePolygonPath(Path2D.Double shape, Graphics2D g2d, double[] polygon, double tolerance) {
        shape.reset();
        if (calculateShape(shape, g2d, polygon, tolerance) >= 3) {
            shape.closePath();
        } else {
            shape.reset();
        }
    }

    public static void prepareComplexPolygon(Path2D.Double finalPath, Graphics2D g2d, double[] outerPolygon, List<double[]> innerPolygons, double tolerance) {
        finalPath.reset();

        int pathsAppended = addPolygonToFinalPath(finalPath, g2d, outerPolygon, tolerance);
        for (var inner : innerPolygons) {
            pathsAppended += addPolygonToFinalPath(finalPath, g2d, inner, tolerance);
        }

        if (pathsAppended == 0) {
            finalPath.reset();
        }
    }
    public static void prepareComplexPolygon(Path2D.Double finalPath, Graphics2D g2d, List<double[]> outerPolygons, List<double[]> innerPolygons, double tolerance) {
        finalPath.reset();
        var pathsAppended = 0;

        for (var outer : outerPolygons) {
            pathsAppended += addPolygonToFinalPath(finalPath, g2d, outer, tolerance);
        }
        for (var inner : innerPolygons) {
            pathsAppended += addPolygonToFinalPath(finalPath, g2d, inner, tolerance);
        }

        if (pathsAppended == 0) {
            finalPath.reset();
        }
    }

    private static int addPolygonToFinalPath(Path2D.Double finalPath, Graphics2D g2d, double[] polygon, double tolerance) {
        var shape = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        if (calculateShape(shape, g2d, polygon, tolerance) >= 3) {
            shape.closePath();
            finalPath.append(shape, false);
            return 1;
        } else {
            return 0;
        }
    }

    private static int calculateShape(Path2D.Double path, Graphics2D g2d, double[] shape, double tolerance) {
        path.moveTo(0.56*shape[0], -shape[1]);

        int pointsAdded = 1, indexLastTransformedPoint = 0;
        double[] pointsTransformed = new double[shape.length];

        g2d.getTransform().transform(shape, 0, pointsTransformed, 0, shape.length/2);

        for (int i = 2; i < pointsTransformed.length; i+=2) {

            if (
                    calculateDistance(
                            pointsTransformed[indexLastTransformedPoint],
                            pointsTransformed[indexLastTransformedPoint+1],
                            pointsTransformed[i],
                            pointsTransformed[i+1]
                    ) <= tolerance
            ) continue;

            path.lineTo(0.56*shape[i], -shape[i+1]);
            indexLastTransformedPoint = i;
            pointsAdded++;
        }

        return pointsAdded;
    }

    private static double calculateDistance(double p1x, double p1y, double p2x, double p2y) {
        return Math.sqrt(
                Math.pow(Math.max(p1x, p2x) - Math.min(p1x, p2x), 2)
                        +
                Math.pow(Math.max(p1y, p2y) - Math.min(p1y, p2y), 2)
        );
    }
}