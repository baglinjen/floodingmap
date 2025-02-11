package dk.itu.drawing.utils;

import dk.itu.drawing.components.BufferedMapComponent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

public class ShapeRasterizer {
    private static final float STEP_SIZE = 0.5f; // Increased step size for better performance

    public static void rasterizeOutline(Path2D.Float path, BufferedMapComponent buffer, int color) {
        AffineTransform affineTransform = buffer.getAffineTransform();

        PathIterator pathIterator = path.getPathIterator(affineTransform);
        float[] coords = new float[6];
        float startX = 0, startY = 0;
        float lastX = 0, lastY = 0;
        boolean first = true;

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    if (first) {
                        startX = coords[0];
                        startY = coords[1];
                        first = false;
                    }
                    lastX = coords[0];
                    lastY = coords[1];
                    break;

                case PathIterator.SEG_LINETO:
                    drawPreciseLine(lastX, lastY, coords[0], coords[1], buffer, color);
                    lastX = coords[0];
                    lastY = coords[1];
                    break;

                case PathIterator.SEG_CLOSE:
                    drawPreciseLine(lastX, lastY, startX, startY, buffer, color);
                    break;
            }
            pathIterator.next();
        }
    }

    private static void drawPreciseLine(float x0, float y0, float x1, float y1, BufferedMapComponent buffer, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < STEP_SIZE) {
            buffer.setPixelSuper(x0, y0, color);
            return;
        }

        float unitX = dx / length;
        float unitY = dy / length;
        float steps = length / STEP_SIZE;

        // Pre-multiply step values
        float stepX = unitX * STEP_SIZE;
        float stepY = unitY * STEP_SIZE;

        float x = x0;
        float y = y0;

        // Unrolled loop for better performance
        int fullSteps = (int) steps;
        for (int i = 0; i <= fullSteps; i++) {
            buffer.setPixelSuper(x, y, color);
            x += stepX;
            y += stepY;
        }
    }
}
