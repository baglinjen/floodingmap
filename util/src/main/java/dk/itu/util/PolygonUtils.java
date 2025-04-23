package dk.itu.util;

import java.util.stream.IntStream;

public class PolygonUtils {
    /**
     * Calculates the exact area of a polygon from an array of coordinates using the shoelace formula
     *
     * @param coordinates Array of coordinates in the format [x1, y1, x2, y2, ...]
     * @return The area of the polygon
     * @throws IllegalArgumentException If input array has odd length or fewer than 6 elements (minimum 3 points)
     */
    public static double calculatePolygonArea(double[] coordinates) {
        // Validate input
        if (coordinates.length % 2 != 0 || coordinates.length < 6) {
            return 0;
        }

        // Apply Shoelace formula directly on the array
        double area = getArea(coordinates);

        // Take the absolute value and divide by 2
        return Math.abs(area) / 2.0;
    }
    private static double getArea(double[] coordinates) {
        double area = 0.0;
        int numPoints = coordinates.length / 2;

        for (int i = 0; i < numPoints; i++) {
            int currentIndex = i * 2;
            int nextIndex = ((i + 1) % numPoints) * 2;

            double currentX = coordinates[currentIndex];
            double currentY = coordinates[currentIndex + 1];
            double nextX = coordinates[nextIndex];
            double nextY = coordinates[nextIndex + 1];

            // Add the cross product
            area += (currentX * nextY) - (nextX * currentY);
        }
        return area;
    }

    public static double[] forceClockwise(double[] polygon) {
        if (isClockwise(polygon)) {
            return polygon;
        } else {
            return reversePairs(polygon);
        }
    }
    public static double[] forceCounterClockwise(double[] polygon) {
        if (isClockwise(polygon)) {
            return reversePairs(polygon);
        } else {
            return polygon;
        }
    }
    private static boolean isClockwise(double[] coordinates) {
        // Ensure we have at least a triangle (6 values: 3 points with x,y each)
        if (coordinates.length < 6 || coordinates.length % 2 != 0) {
            return true;
        }

        // Calculate signed area using the shoelace formula
        double sum = 0;
        int n = coordinates.length / 2;

        for (int i = 0; i < n; i++) {
            // Get current and next vertex (wrapping around for the last vertex)
            int j = (i + 1) % n;

            // Get coordinates
            double x1 = coordinates[i * 2];
            double y1 = coordinates[i * 2 + 1];
            double x2 = coordinates[j * 2];
            double y2 = coordinates[j * 2 + 1];

            // Add cross product
            sum += (x1 * y2) - (y1 * x2);
        }

        // If sum is positive, the polygon is counterclockwise
        // If sum is negative, the polygon is clockwise
        return sum < 0;
    }

    public static double[] reversePairs(double[] array) {
        // Check if array has an even number of elements
        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("Array length must be even to keep pairs together");
        }

        // Create result array of the same size
        double[] result = new double[array.length];

        // Reverse the array pair by pair
        for (int i = 0; i < array.length / 2; i++) {
            // Get the index of the source pair (from the end)
            int sourceIndex = array.length - 2 - (i * 2);

            // Get the index of the destination pair (from the beginning)
            int destIndex = i * 2;

            // Copy the pair
            result[destIndex] = array[sourceIndex];
            result[destIndex + 1] = array[sourceIndex + 1];
        }

        return result;
    }

    public static boolean contains(double[] p1, double[] p2) {
        // All p2 points should be in p1
        return IntStream.range(0, (p2.length-1) / 2)
                .parallel()
                .allMatch(i -> {
                    //
                    return isPointInPolygon(p1, p2[i*2], p2[i*2+1]);
                });
    }

    public static boolean isClosed(double[] coords) {
        return coords[0] == coords[coords.length - 2] && coords[1] == coords[coords.length - 1];
    }

    public static OpenPolygonMatchType findOpenPolygonMatchType(double[] polygon1, double[] polygon2) {
        if (polygon1[0] == polygon2[0] && polygon1[1] == polygon2[1]) {
            return OpenPolygonMatchType.FIRST_FIRST;
        } else if (polygon1[0] == polygon2[polygon2.length - 2] && polygon1[1] == polygon2[polygon2.length - 1]) {
            return OpenPolygonMatchType.FIRST_LAST;
        } else if (polygon1[polygon1.length - 2] == polygon2[0] && polygon1[polygon1.length - 1] == polygon2[1]) {
            return OpenPolygonMatchType.LAST_FIRST;
        } else if (polygon1[polygon1.length - 2] == polygon2[polygon2.length - 2] && polygon1[polygon1.length - 1] == polygon2[polygon2.length - 1]) {
            return OpenPolygonMatchType.LAST_LAST;
        } else {
            return OpenPolygonMatchType.NONE;
        }
    }

    public enum OpenPolygonMatchType {
        FIRST_FIRST,
        FIRST_LAST,
        LAST_FIRST,
        LAST_LAST,
        NONE
    }

    /**
     * Checks if the second polygon is fully contained within the first polygon using ray casting.
     *
     * @param polygon1 Array of doubles representing vertices of first polygon in form [x1,y1,x2,y2,...,xn,yn]
     * @param polygon2 Array of doubles representing vertices of second polygon in form [x1,y1,x2,y2,...,xn,yn]
     * @return true if polygon2 is fully contained within polygon1, false otherwise
     */
    public static boolean isPolygonContained(double[] polygon1, double[] polygon2) {
        // Validate input
        if (polygon1 == null || polygon2 == null || polygon1.length < 6 || polygon2.length < 6) {
            throw new IllegalArgumentException("Polygons must have at least 3 vertices (6 values)");
        }

        if (polygon1.length % 2 != 0 || polygon2.length % 2 != 0) {
            throw new IllegalArgumentException("Polygon arrays must have even length");
        }

        // First check: all vertices of polygon2 must be inside polygon1
        for (int i = 0; i < polygon2.length; i += 2) {
            double x = polygon2[i];
            double y = polygon2[i + 1];

            if (!isPointInPolygon(polygon1, x, y)) {
                return false;
            }
        }

        // Second check: no edges of polygon2 intersect with edges of polygon1
        int n1 = polygon1.length / 2;
        int n2 = polygon2.length / 2;

        for (int i = 0; i < n2; i++) {
            int j = (i + 1) % n2;
            double x1 = polygon2[2*i];
            double y1 = polygon2[2*i + 1];
            double x2 = polygon2[2*j];
            double y2 = polygon2[2*j + 1];

            for (int k = 0; k < n1; k++) {
                int l = (k + 1) % n1;
                double x3 = polygon1[2*k];
                double y3 = polygon1[2*k + 1];
                double x4 = polygon1[2*l];
                double y4 = polygon1[2*l + 1];

                if (doSegmentsIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) {
                    // If edges intersect (not just at vertices), polygon2 is not contained
                    if (!isVertexIntersection(x1, y1, x2, y2, x3, y3, x4, y4)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean isPointInPolygon(double[] polygon, double x, double y) {
        boolean inside = false;

        double x1 = polygon[polygon.length - 2];
        double y1 = polygon[polygon.length - 1];
        double x2, y2;

        for (int i = 0; i < polygon.length; i+=2) {
            x2 = polygon[i];
            y2 = polygon[i + 1];

            if (
                    ((y1 > y) != (y2 > y)) && // Condition 1 within y bounds
                    (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)  // Condition 2 within x bounds
            ) {
                inside = !inside;
            }

            x1 = x2;
            y1 = y2;
        }

        return inside;
    }

    /**
     * Determines if two line segments intersect.
     */
    private static boolean doSegmentsIntersect(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {
        // Calculate the direction of the segments
        double d1x = x2 - x1;
        double d1y = y2 - y1;
        double d2x = x4 - x3;
        double d2y = y4 - y3;

        // Calculate the determinant
        double denominator = d1y * d2x - d1x * d2y;

        // If lines are parallel
        if (Math.abs(denominator) < 1e-10) {
            return false;
        }

        // Calculate intersection parameters
        double dx = x3 - x1;
        double dy = y3 - y1;
        double t = (dx * d2y - dy * d2x) / denominator;
        double u = (dx * d1y - dy * d1x) / denominator;

        // Check if intersection is within segments
        return (t >= 0 && t <= 1 && u >= 0 && u <= 1);
    }

    /**
     * Checks if the intersection of two segments is only at their vertices.
     */
    private static boolean isVertexIntersection(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4
    ) {
        // Check if any endpoint of one segment coincides with any endpoint of the other
        return (isPointEqual(x1, y1, x3, y3) || isPointEqual(x1, y1, x4, y4) ||
                isPointEqual(x2, y2, x3, y3) || isPointEqual(x2, y2, x4, y4));
    }

    /**
     * Checks if two points are the same within a small epsilon.
     */
    private static boolean isPointEqual(double x1, double y1, double x2, double y2) {
        double epsilon = 1e-10; // Small tolerance for floating-point comparison
        return Math.abs(x1 - x2) < epsilon && Math.abs(y1 - y2) < epsilon;
    }

    /**
     * @param x Longitude
     * @param y Latitude
     * @param bounds [minX, minY, maxX, maxY]
     * @return True if point in bounds or intersects
     */
    public static boolean pointInBounds(double x, double y, double[] bounds) {
        return x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3];
    }
}
