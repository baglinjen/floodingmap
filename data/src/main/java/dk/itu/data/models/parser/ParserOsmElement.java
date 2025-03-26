package dk.itu.data.models.parser;

import dk.itu.common.models.Geographical2D;
import dk.itu.common.models.OsmElement;

import java.awt.geom.Path2D;

public abstract class ParserOsmElement extends ParserDrawable implements OsmElement, Geographical2D {
    private final long id;

    public ParserOsmElement(long id) {
        this.id = id;
    }

    @Override
    public long id() {
        return id;
    }

    public abstract double getArea();
    public abstract Path2D.Double getShape();

    protected static boolean isClockwise(double[] coordinates) {
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

    protected static double[] reversePairs(double[] array) {
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
}

