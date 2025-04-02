package dk.itu.common.models;

import dk.itu.common.configurations.DrawingConfiguration;

import java.awt.*;

import static dk.itu.util.DrawingUtils.toARGB;

public interface GeoJsonElement extends Drawable {
    DrawingConfiguration.Style styleBelowWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e80")), true), 1);
    DrawingConfiguration.Style styleSelected = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#00FF0080")), true), 1);
    float getHeight();
    void setBelowWater(boolean belowWater);
}