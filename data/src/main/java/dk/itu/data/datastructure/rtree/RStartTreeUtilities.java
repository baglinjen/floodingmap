package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.WithBoundingBoxAndArea;

import static dk.itu.common.models.WithBoundingBoxAndArea.calculateArea;

public class RStartTreeUtilities {
    public static double getEnlargementArea(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return calculateArea(
                Math.min(b1.minLon(), b2.minLon()),
                Math.min(b1.minLat(), b2.minLat()),
                Math.max(b1.maxLon(), b2.maxLon()),
                Math.max(b1.maxLat(), b2.maxLat())
        );
    }

    public static double getOverlap(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return
                Math.max(Math.abs(b1.minLon()) - b2.maxLon(), Math.abs(b1.maxLon() - b2.minLon()))
                *
                Math.max(Math.abs(b1.minLat() - b2.maxLat()), Math.abs(b1.maxLat() - b2.minLat()));
    }

    public static double getCenterOfAxis(WithBoundingBoxAndArea boundingBox, boolean isLonAxis) {
        return isLonAxis ?
                (boundingBox.minLon() + boundingBox.maxLon()) / 2 :
                (boundingBox.minLat() + boundingBox.maxLat()) / 2;
    }

    public static double getDistance(WithBoundingBoxAndArea box, double centerLon, double centerLat) {
        return pointDistance(getCenterOfAxis(box, true), getCenterOfAxis(box, false), centerLon, centerLat);
    }

    /**
     * Calculate the exact distance between two points
     * @param x1 First point x coordinate
     * @param y1 First point y coordinate
     * @param x2 Second point x coordinate
     * @param y2 Second point y coordinate
     * @return The Euclidean distance between the points
     */
    public static double pointDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        // Euclidean distance
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static boolean intersects(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return intersects(b1, b2.minLon(), b2.minLat(), b2.maxLon(), b2.maxLat());

    }
    public static boolean intersects(WithBoundingBoxAndArea b1, double minLon, double minLat, double maxLon, double maxLat) {
        return !(
                b1.minLon() > maxLon ||
                b1.maxLon() < minLon ||
                b1.minLat() > maxLat ||
                b1.maxLat() < minLat
        );
    }

    public static WithBoundingBoxAndArea createWithBoundingBoxAndArea(double minLon, double minLat, double maxLon, double maxLat) {
        return new WithBoundingBoxAndArea() {
            @Override
            public double minLon() {
                return minLon;
            }

            @Override
            public double minLat() {
                return minLat;
            }

            @Override
            public double maxLon() {
                return maxLon;
            }

            @Override
            public double maxLat() {
                return maxLat;
            }

            @Override
            public double getArea() {
                return (maxLon - minLon) * (maxLat - minLat);
            }
        };
    }
}
