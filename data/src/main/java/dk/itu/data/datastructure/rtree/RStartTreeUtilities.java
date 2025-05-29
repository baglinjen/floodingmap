package dk.itu.data.datastructure.rtree;

import dk.itu.common.models.WithBoundingBoxAndArea;

import static dk.itu.common.models.WithBoundingBoxAndArea.calculateArea;

public class RStartTreeUtilities {
    public static float getEnlargementArea(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return getEnlargedArea(b1, b2) - b1.getArea();
    }

    public static float getEnlargedArea(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return calculateArea(
                Math.min(b1.minLon(), b2.minLon()),
                Math.min(b1.minLat(), b2.minLat()),
                Math.max(b1.maxLon(), b2.maxLon()),
                Math.max(b1.maxLat(), b2.maxLat())
        );
    }

    public static float getOverlap(WithBoundingBoxAndArea b1, WithBoundingBoxAndArea b2) {
        return
                Math.max(Math.abs(b1.minLon()) - b2.maxLon(), Math.abs(b1.maxLon() - b2.minLon()))
                *
                Math.max(Math.abs(b1.minLat() - b2.maxLat()), Math.abs(b1.maxLat() - b2.minLat()));
    }

    public static float getCenterOfAxis(WithBoundingBoxAndArea boundingBox, boolean isLonAxis) {
        return isLonAxis ?
                (boundingBox.minLon() + boundingBox.maxLon()) / 2 :
                (boundingBox.minLat() + boundingBox.maxLat()) / 2;
    }

    public static float getDistance(WithBoundingBoxAndArea box, float centerLon, float centerLat) {
        return (float) pointDistance(getCenterOfAxis(box, true), getCenterOfAxis(box, false), centerLon, centerLat);
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
    public static boolean intersects(WithBoundingBoxAndArea b1, float minLon, float minLat, float maxLon, float maxLat) {
        return !(
                b1.minLon() > maxLon ||
                b1.maxLon() < minLon ||
                b1.minLat() > maxLat ||
                b1.maxLat() < minLat
        );
    }

    public static WithBoundingBoxAndArea createWithBoundingBoxAndArea(float minLon, float minLat, float maxLon, float maxLat) {
        return new WithBoundingBoxAndArea() {
            @Override
            public float minLon() {
                return minLon;
            }

            @Override
            public float minLat() {
                return minLat;
            }

            @Override
            public float maxLon() {
                return maxLon;
            }

            @Override
            public float maxLat() {
                return maxLat;
            }

            @Override
            public float getArea() {
                return (maxLon - minLon) * (maxLat - minLat);
            }
        };
    }
}
