package dk.itu.drawing.utils;

import dk.itu.drawing.components.BufferedMapComponent;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShapeRasterizer {
    private static final float STEP_SIZE = 0.5f; // Increased step size for better performance

    public static void drawShapeInBuffer(Shape shape, BufferedMapComponent buffer, int color) {
        switch (shape) {
            case Area area:
                drawArea(area, buffer, color);
                break;
            case Path2D.Float path:
                drawPath(path, buffer, color);
                break;
            default:
                throw new IllegalArgumentException("Unsupported shape " + shape);
        }
    }

    private static void drawArea(Area area, BufferedMapComponent buffer, int color) {
        // Fill the area
        PathIterator pathIterator = area.getPathIterator(buffer.getAffineTransform());
        float[] coords = new float[6];
        List<Edge> edges = new ArrayList<>();
        float startX = 0, startY = 0;
        float lastX = 0, lastY = 0;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;

        // Build edge table
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    startX = lastX = coords[0];
                    startY = lastY = coords[1];
                    break;

                case PathIterator.SEG_LINETO:
                    if (Math.abs(lastY - coords[1]) > 0.001f) {  // Skip horizontal lines
                        edges.add(new Edge(lastX, lastY, coords[0], coords[1]));
                        minY = Math.min(minY, Math.min(lastY, coords[1]));
                        maxY = Math.max(maxY, Math.max(lastY, coords[1]));
                    }
                    lastX = coords[0];
                    lastY = coords[1];
                    break;

                case PathIterator.SEG_CLOSE:
                    if (Math.abs(lastY - startY) > 0.001f) {  // Skip horizontal lines
                        edges.add(new Edge(lastX, lastY, startX, startY));
                        minY = Math.min(minY, Math.min(lastY, startY));
                        maxY = Math.max(maxY, Math.max(lastY, startY));
                    }
                    break;
            }
            pathIterator.next();
        }

        // Sort edges by minimum y coordinate
        Collections.sort(edges);

        // Active edge list
        List<Edge> activeEdges = new ArrayList<>();

        // Scan line algorithm
        for (float y = minY; y <= maxY; y += STEP_SIZE) {
            // Remove completed edges
            float finalY = y;
            activeEdges.removeIf(edge -> finalY >= edge.yMax);

            // Add new edges that start at this scanline
            for (Edge edge : edges) {
                if (y >= edge.yMin && y < edge.yMax && !activeEdges.contains(edge)) {
                    activeEdges.add(edge);
                }
            }

            // Sort active edges by x intersection
            activeEdges.sort((e1, e2) -> Float.compare(e1.xIntersect(finalY), e2.xIntersect(finalY)));

            // Fill between pairs of intersections
            for (int i = 0; i < activeEdges.size() - 1; i += 2) {
                if (i + 1 < activeEdges.size()) {
                    float x1 = activeEdges.get(i).xIntersect(y);
                    float x2 = activeEdges.get(i + 1).xIntersect(y);
                    for (float x = x1; x <= x2; x += STEP_SIZE) {
                        buffer.setPixelSuper(x, y, color);
                    }
                }
            }
        }

        // Draw the outline
        pathIterator = area.getPathIterator(buffer.getAffineTransform());
        lastX = lastY = startX = startY = 0;
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

    private static class Edge implements Comparable<Edge> {
        final float x1, y1, x2, y2;
        final float yMin, yMax;
        final float inverseSlope;

        Edge(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.yMin = Math.min(y1, y2);
            this.yMax = Math.max(y1, y2);
            this.inverseSlope = (x2 - x1) / (y2 - y1);
        }

        float xIntersect(float y) {
            return x1 + (y - y1) * inverseSlope;
        }

        @Override
        public int compareTo(Edge other) {
            return Float.compare(this.yMin, other.yMin);
        }
    }

    private static void drawPath(Path2D.Float path, BufferedMapComponent buffer, int color) {
        PathIterator pathIterator = path.getPathIterator(buffer.getAffineTransform());
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
