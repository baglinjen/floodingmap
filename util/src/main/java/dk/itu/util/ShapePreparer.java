package dk.itu.util;

import kotlin.Pair;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
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

        List<double[]> polygons = new ArrayList<>();
        polygons.addAll(outerPolygons);
        polygons.addAll(innerPolygons);

        for (double[] polygon : polygons) {
            var shape = calculateShape(g2d, polygon, tolerance);

            if (shape.component2() >= 3) {
                shape.component1().closePath();
                finalPath.append(shape.component1(), false);
                pathsAppended++;
            }
        }

        return pathsAppended > 0 ? finalPath : null;
    }

    private static Pair<Path2D.Double, Integer> calculateShape(Graphics2D g2d, double[] shape, double tolerance) {
        var path = new Path2D.Double();
        var transform = g2d.getTransform();

        path.moveTo(0.56*shape[0], -shape[1]);
        int pointsAdded = 1;

        var lastTransformedPoint = new Point2D.Double();
        transform.transform(new Point2D.Double(shape[0], shape[1]), lastTransformedPoint);

        for (int i = 2; i < shape.length; i+=2) {
            var point = new Point2D.Double(shape[i], shape[i+1]);
            var transformedPoint = new Point2D.Double();
            transform.transform(point, transformedPoint);

            boolean isSamePixel = transformedPoint.distance(lastTransformedPoint) <= tolerance;
            if (isSamePixel) continue;

            path.lineTo(0.56*point.getX(), -point.getY());
            lastTransformedPoint = transformedPoint;
            pointsAdded++;
        }

        return new Pair<>(path, pointsAdded);
    }
}