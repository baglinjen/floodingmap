package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

import static dk.itu.util.DrawingUtils.toARGB;

public interface GeoJsonElement extends Drawable {
    DrawingConfiguration.Style styleBelowWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e80")), true), 1);
    DrawingConfiguration.Style styleAboveWater = new DrawingConfiguration.Style(Color.BLACK, 1);
    float getHeight();
    public void setBelowWater(boolean belowWater);
}
