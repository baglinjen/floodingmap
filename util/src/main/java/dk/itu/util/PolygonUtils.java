package dk.itu.util;

import java.util.Arrays;
import java.util.stream.IntStream;

public class PolygonUtils {
    @FunctionalInterface
    private interface CoordinateAccessor {
        double get(int index);
    }
    private static double calculateShoelaceSum(CoordinateAccessor coords, int length) {
        var area = 0.0;
        int numPoints = length / 2;

        for (int i = 0; i < numPoints; i++) {
            int currentIndex = i * 2;
            int nextIndex = ((i + 1) % numPoints) * 2;

            double currentX = coords.get(currentIndex);
            double currentY = coords.get(currentIndex + 1);
            double nextX = coords.get(nextIndex);
            double nextY = coords.get(nextIndex + 1);

            // Add the cross product
            area += (currentX * nextY) - (nextX * currentY);
        }
        return area;
    }

    public static double[] forceCounterClockwise(double[] polygon) {
        if (isClockwise(polygon)) {
            return reversePairs(polygon);
        } else {
            return polygon;
        }
    }
    public static float[] forceCounterClockwise(float[] polygon) {
        if (isClockwise(polygon)) {
            return reversePairs(polygon);
        } else {
            return polygon;
        }
    }
    private static boolean isClockwise(double[] coordinates) {
        // Negative area means it is clockwise
        return calculateShoelaceSum(i -> coordinates[i], coordinates.length) < 0;
    }
    private static boolean isClockwise(float[] coordinates) {
        // Negative area means it is clockwise
        return calculateShoelaceSum(i -> coordinates[i], coordinates.length) < 0;
    }

    public static double[] reversePairs(double[] array) {
        double[] result = Arrays.copyOf(array, array.length);
        reversePairsMut(result);
        return result;
    }
    public static float[] reversePairs(float[] array) {
        float[] result = Arrays.copyOf(array, array.length);
        reversePairsMut(result);
        return result;
    }
    public static void reversePairsMut(double[] array) {
        // Check if array has an even number of elements
        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("Array length must be even to keep pairs together");
        }

        int indexEnd;
        double intermediateX, intermediateY;
        for (int i = 0; i < array.length / 2; i+=2) {
            indexEnd = array.length - 2 - i;

            if (i == indexEnd) continue;

            intermediateX = array[i];
            intermediateY = array[i + 1];
            array[i] = array[indexEnd];
            array[i + 1] = array[indexEnd + 1];
            array[indexEnd] = intermediateX;
            array[indexEnd + 1] = intermediateY;
        }
    }
    public static void reversePairsMut(float[] array) {
        // Check if array has an even number of elements
        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("Array length must be even to keep pairs together");
        }

        int indexEnd;
        float intermediateX, intermediateY;
        for (int i = 0; i < array.length / 2; i+=2) {
            indexEnd = array.length - 2 - i;

            if (i == indexEnd) continue;

            intermediateX = array[i];
            intermediateY = array[i + 1];
            array[i] = array[indexEnd];
            array[i + 1] = array[indexEnd + 1];
            array[indexEnd] = intermediateX;
            array[indexEnd + 1] = intermediateY;
        }
    }


    public static boolean contains(double[] p1, double[] p2) {
        final boolean[] foundOutsideAsync = {false};
        IntStream.range(0, p2.length / 2 - 1)
                .parallel()
                .forEach(i -> {
                    if (!foundOutsideAsync[0] && !isPointInPolygon(p1, p2[i*2], p2[i*2 + 1])) foundOutsideAsync[0] = true;
                });
        return !foundOutsideAsync[0];
    }
    public static boolean contains(float[] p1, float[] p2) {
        final boolean[] foundOutsideAsync = {false};
        IntStream.range(0, p2.length / 2 - 1)
                .parallel()
                .forEach(i -> {
                    if (!foundOutsideAsync[0] && !isPointInPolygon(p1, p2[i*2], p2[i*2 + 1])) foundOutsideAsync[0] = true;
                });
        return !foundOutsideAsync[0];
    }

    public static boolean isClosed(double[] coords) {
        return coords[0] == coords[coords.length - 2] && coords[1] == coords[coords.length - 1];
    }
    public static boolean isClosed(float[] coords) {
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
    public static OpenPolygonMatchType findOpenPolygonMatchType(float[] polygon1, float[] polygon2) {
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

    /**
     * Checks if the second polygon is fully contained within the first polygon using ray casting.
     *
     * @param polygon1 Array of doubles representing vertices of first polygon in form [x1,y1,x2,y2,...,xn,yn]
     * @param polygon2 Array of doubles representing vertices of second polygon in form [x1,y1,x2,y2,...,xn,yn]
     * @return true if polygon2 is fully contained within polygon1, false otherwise
     */
    public static boolean isPolygonContained(float[] polygon1, float[] polygon2) {
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
            float x1 = polygon2[2*i];
            float y1 = polygon2[2*i + 1];
            float x2 = polygon2[2*j];
            float y2 = polygon2[2*j + 1];

            for (int k = 0; k < n1; k++) {
                int l = (k + 1) % n1;
                float x3 = polygon1[2*k];
                float y3 = polygon1[2*k + 1];
                float x4 = polygon1[2*l];
                float y4 = polygon1[2*l + 1];

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

    public static boolean isPointInPolygon(float[] polygon, double x, double y) {
        boolean inside = false;

        float x1 = polygon[polygon.length - 2];
        float y1 = polygon[polygon.length - 1];
        float x2, y2;

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
}