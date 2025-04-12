package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

public abstract class Colored implements Drawable {
    public static double DRAWING_TOLERANCE = 8;
    private Color rgbaColor;
    private Integer stroke;

    public void setStyle(DrawingConfiguration.Style style) {
        if (style != null) {
            this.rgbaColor = style.rgba();
            this.stroke = style.stroke();
        } else {
            this.rgbaColor = null;
            this.stroke = null;
        }
    }
    public void setStyle(Color rgbaColor, Integer stroke) { this.rgbaColor = rgbaColor; this.stroke = stroke; }

    public Color getRgbaColor() {
        return rgbaColor;
    }
    public Integer getStroke() {
        return stroke;
    }
}