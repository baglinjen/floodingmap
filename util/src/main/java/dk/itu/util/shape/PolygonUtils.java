package dk.itu.util.shape;

public class PolygonUtils {
    /**
     * Checks if the second polygon is fully contained within the first polygon.
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
     * Checks if a point is inside a polygon using the ray casting algorithm.
     */
    private static boolean isPointInPolygon(double[] polygon, double x, double y) {
        boolean inside = false;
        int numVertices = polygon.length / 2;

        for (int i = 0, j = numVertices - 1; i < numVertices; j = i++) {
            double xi = polygon[2*i];
            double yi = polygon[2*i + 1];
            double xj = polygon[2*j];
            double yj = polygon[2*j + 1];

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
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
            double x3, double y3, double x4, double y4) {
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
