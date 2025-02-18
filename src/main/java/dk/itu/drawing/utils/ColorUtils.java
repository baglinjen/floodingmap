package dk.itu.drawing.utils;

import javafx.scene.paint.Color;
import java.util.Random;

public class ColorUtils {
    private static final Random random = new Random();

    public static int toARGB(Color color) {
        return (int) (color.getOpacity() * 255) << 24
                | (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) <<  8
                | (int) (color.getBlue() * 255);
    }
    public static int generateColorArgb() {
        return 0xFF000000  // Full opacity
                | random.nextInt(256) << 16    // Red
                | random.nextInt(256) << 8     // Green
                | random.nextInt(256);         // Blue
    }


}
