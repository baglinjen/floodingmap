package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

public interface Drawable {
    double DRAWING_AREA_TOLERANCE = 32;
    void setStyle(DrawingConfiguration.Style style);
    void prepareDrawing(Graphics2D g2d);
    boolean shouldDraw();
    void draw(Graphics2D g2d, float strokeBaseWidth);
}