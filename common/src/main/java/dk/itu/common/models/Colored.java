package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

public abstract class Colored implements Drawable {
    public static double DRAWING_TOLERANCE = 8;
    private Color color;
    private Integer stroke;

    public void setStyle(DrawingConfiguration.Style style) {
        if (style != null) {
            this.color = style.rgba();
            this.stroke = style.stroke();
        } else {
            this.color = null;
            this.stroke = null;
        }
    }
    public void setStyle(Color rgbaColor, Integer stroke) { this.color = rgbaColor; this.stroke = stroke; }

    public Color getColor() {
        return color;
    }
    public Integer getStroke() {
        return stroke;
    }
}