package dk.itu.util;

import javafx.scene.paint.Color;

public class DrawingUtils {
    public static final double DRAWING_TOLERANCE = 8;
    public static double calculateDistance(double p1x, double p1y, double p2x, double p2y) {
        return Math.sqrt(
                Math.pow(Math.max(p1x, p2x) - Math.min(p1x, p2x), 2)
                +
                Math.pow(Math.max(p1y, p2y) - Math.min(p1y, p2y), 2)
        );
    }
    /**
     * Converts a color to its integer rgba representation
     */
    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
}