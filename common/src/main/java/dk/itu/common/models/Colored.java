package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

public abstract class Colored implements Drawable {
    public static double DRAWING_TOLERANCE = 8;
    private Color rgbaColor;
    private float stroke = 1;

    public void setStyle(DrawingConfiguration.Style style) {
        if (style != null) {
            var rgba = style.rgba();
            if (rgba != null) {
                this.rgbaColor = rgba;
            }
            var stroke = style.stroke();
            if (stroke != null) {
                this.stroke = stroke;
            }
        } else {
            this.rgbaColor = null;
        }
    }

    public Color getRgbaColor() {
        return rgbaColor;
    }
    public float getStroke() {
        return stroke;
    }
    public void setStyle(Color rgbaColor, float stroke) { this.rgbaColor = rgbaColor; this.stroke = stroke; }
}