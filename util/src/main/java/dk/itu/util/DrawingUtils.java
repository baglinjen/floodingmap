package dk.itu.util;

import javafx.scene.paint.Color;

public class DrawingUtils {
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