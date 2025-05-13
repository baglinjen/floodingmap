package dk.itu.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;

public class DrawingUtils {
    public static WritableImage createWritableImageFromBufferedImage(BufferedImage img) {
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        bufferedImage.createGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);

        int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        IntBuffer b = IntBuffer.wrap(pixels);

        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                b,
                PixelFormat.getIntArgbPreInstance()
        );

        return new WritableImage(pixelBuffer);
    }

    /**
     * Uses BufferedImage to create a WritableImage UI component
     */
    public static void transferBufferedImageDataToWritableImage(BufferedImage bufferedImage, WritableImage writableImage) {

        int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        IntBuffer b = IntBuffer.wrap(pixels);

        //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                b,
                PixelFormat.getIntArgbPreInstance()
        );

//        return new WritableImage(pixelBuffer);


//        byte[] pixelData = new byte[width * height * 4];
//
//        IntStream.range(0, pixels.length).parallel().forEach(i -> {
//            var pixel = pixels[i];
//            pixelData[i * 4] = (byte) ((pixel) & 0xFF);         // Blue
//            pixelData[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);  // Green
//            pixelData[i * 4 + 2] = (byte) ((pixel >> 16) & 0xFF); // Red
//            pixelData[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // Alpha
//        });



//        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
//                width, height,
//                ByteBuffer.wrap(pixelData),
//                PixelFormat.getByteBgraPreInstance()
//        );

//        buffer.clear();
//        buffer.position(0);
//
//        buffer.put(pixels, 0, pixels.length);

//        writableImage.getPixelWriter().setPixels(
//                0, 0,
//                width, height,
//                PixelFormat.getIntArgbPreInstance(),
//                pixels,
//                0, 0
//        );

//        return new WritableImage(pixelBuffer);
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