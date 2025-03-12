package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

public abstract class Drawable {
    private boolean shouldBeDrawn = true;
    private Color rgbaColor;
    private float stroke = 1;

    public void setShouldBeDrawn(boolean shouldBeDrawn) {
        this.shouldBeDrawn = shouldBeDrawn;
    }

    public boolean shouldBeDrawn() {
        return shouldBeDrawn;
    }

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

    public abstract void draw(Graphics2D g2d, float strokeBaseWidth);

    public Color getRgbaColor() {
        return rgbaColor;
    }

    public float getStroke() {
        return stroke;
    }
}
