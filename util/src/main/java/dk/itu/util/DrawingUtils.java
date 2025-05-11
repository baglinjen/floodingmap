package dk.itu.util;

import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class DrawingUtils {
    

    /**
     * Uses BufferedImage to create a WritableImage UI component
     */
    public static WritableImage bufferedImageToWritableImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        byte[] pixelData = new byte[width * height * 4];

        IntStream.range(0, pixels.length).parallel().forEach(i -> {
            var pixel = pixels[i];
            pixelData[i * 4] = (byte) ((pixel) & 0xFF);         // Blue
            pixelData[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);  // Green
            pixelData[i * 4 + 2] = (byte) ((pixel >> 16) & 0xFF); // Red
            pixelData[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // Alpha
        });

        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
                width, height,
                ByteBuffer.wrap(pixelData),
                PixelFormat.getByteBgraPreInstance()
        );

        return new WritableImage(pixelBuffer);
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