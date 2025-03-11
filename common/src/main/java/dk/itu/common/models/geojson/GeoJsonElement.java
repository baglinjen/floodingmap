package dk.itu.common.models.geojson;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;

import java.awt.*;
import java.awt.geom.Path2D;

import static dk.itu.util.DrawingUtils.toARGB;

public class GeoJsonElement extends Drawable {
    private static final DrawingConfiguration.Style styleAboveWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#ff450020")), true), 1);
    private static final DrawingConfiguration.Style styleBelowWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e20")), true), 1);
    private final float height;
    private final Shape shape;

    public GeoJsonElement(float height, Path2D path) {
        this.height = height;
        this.shape = path;
        setShouldBeDrawn(true);
    }

    public float getHeight() {
        return height;
    }

    public GeoJsonElement updateStyle(float waterLevel) {
        setStyle(waterLevel >= height ? styleBelowWater : styleAboveWater);
        return this;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        g2d.fill(shape);
    }
}
