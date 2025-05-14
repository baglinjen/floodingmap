package dk.itu.util;

import kotlin.Pair;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

public class ShapePreparer {
    public static Path2D.Double prepareLinePath(Graphics2D g2d, double[] line, double tolerance) {
        var shape = calculateShape(g2d, line, tolerance);
        return shape.component2() > 2 ? shape.component1() : null;
    }

    public static Path2D.Double preparePolygonPath(Graphics2D g2d, double[] polygon, double tolerance) {
        var shape = calculateShape(g2d, polygon, tolerance);
        if (shape.component2() >= 3) {
            shape.component1().closePath();
            return shape.component1();
        } else {
            return null;
        }
    }

    public static Path2D.Double prepareComplexPolygon(Graphics2D g2d, List<double[]> outerPolygons, List<double[]> innerPolygons, double tolerance) {
        var finalPath = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        var pathsAppended = 0;

        for (var outer : outerPolygons) {
            var shape = calculateShape(g2d, outer, tolerance);

            if (shape.component2() >= 3) {
                shape.component1().closePath();
                finalPath.append(shape.component1(), false);
                pathsAppended++;
            }
        }
        for (var inner : innerPolygons) {
            var shape = calculateShape(g2d, inner, tolerance);

            if (shape.component2() >= 3) {
                shape.component1().closePath();
                finalPath.append(shape.component1(), false);
                pathsAppended++;
            }
        }

        return pathsAppended > 0 ? finalPath : null;
    }

    // Pair with path2D and number of vertices
    private static Pair<Path2D.Double, Integer> calculateShape(Graphics2D g2d, double[] shape, double tolerance) {
        var path = new Path2D.Double();
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

        return new Pair<>(path, pointsAdded);
    }

    private static double calculateDistance(double p1x, double p1y, double p2x, double p2y) {
        return Math.sqrt(
                Math.pow(Math.max(p1x, p2x) - Math.min(p1x, p2x), 2)
                        +
                Math.pow(Math.max(p1y, p2y) - Math.min(p1y, p2y), 2)
        );
    }
}