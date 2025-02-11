package dk.itu.drawing.components;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Affine;

import java.awt.geom.AffineTransform;
import java.nio.IntBuffer;
import java.util.Arrays;

public class BufferedMapComponent extends ImageView {
    private final int superSampleFactor, scaledWidth, scaledHeight;
    private final int[] rawPixelIntegers;
    private final PixelBuffer<IntBuffer> pixelBuffer;
    private final Affine affine;

    // Pre-calculated offsets for each row to avoid multiplication in inner loop
    private final int[] rowOffsets;

    public BufferedMapComponent(int width, int height, int superSampleFactor, Affine affine) {
        this.superSampleFactor = superSampleFactor;
        this.scaledWidth = width * superSampleFactor;
        this.scaledHeight = height * superSampleFactor;
        this.affine = affine;

        // Create scaled pixel buffer
        IntBuffer buffer = IntBuffer.allocate(scaledWidth * scaledHeight);
        rawPixelIntegers = buffer.array();
        pixelBuffer = new PixelBuffer<>(scaledWidth, scaledHeight, buffer, PixelFormat.getIntArgbPreInstance());

        // Set component to display buffer contents
        setImage(new WritableImage(pixelBuffer));
        setFitWidth(width);
        setFitHeight(height);
        setPreserveRatio(false);

        // Pre-calculate row offsets
        rowOffsets = new int[scaledHeight];
        for (int i = 0; i < scaledHeight; i++) {
            rowOffsets[i] = i * scaledWidth;
        }
    }

    public void setPixelSuper(float inputX, float inputY, int colorARGB) {
        int baseX = (int)(inputX * superSampleFactor);
        int baseY = (int)(inputY * superSampleFactor);

        // Early bounds check
        if (baseX < 0 || baseX >= scaledWidth - superSampleFactor ||
                baseY < 0 || baseY >= scaledHeight - superSampleFactor) {
            return;
        }

        int endY = baseY + superSampleFactor;
        for (int y = baseY; y < endY; y++) {
            int offset = rowOffsets[y] + baseX;
            rawPixelIntegers[offset] = colorARGB;
            rawPixelIntegers[offset + 1] = colorARGB;
        }
    }

    public void clear(int backgroundColor) {
        Arrays.fill(rawPixelIntegers, backgroundColor);
    }

    public BufferedMapComponent updateBuffer() {
        pixelBuffer.updateBuffer(_ -> null);
        return this;
    }

    public AffineTransform getAffineTransform() {
        return new AffineTransform(
            affine.getMxx(), affine.getMyx(),
            affine.getMxy(), affine.getMyy(),
            affine.getTx(), affine.getTy()
        );
    }
}
