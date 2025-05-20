package dk.itu.data.models.heightcurve;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.data.models.BoundingBox;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.PolygonUtils;
import dk.itu.util.shape.HeightCurvePath;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.DrawingUtils.toARGB;
import static dk.itu.util.PolygonUtils.forceCounterClockwise;

public class HeightCurveElement extends BoundingBox {
    private static final DrawingConfiguration.Style STYLE_ABOVE_WATER = new DrawingConfiguration.Style(Color.BLACK, 1);
    private static final DrawingConfiguration.Style STYLE_BELOW_WATER = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e80")), true), null);
    private static final DrawingConfiguration.Style STYLE_SELECTED = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#00FF0080")), true), null);
    private final double[] outerPolygon;
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final float height;
    private final Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
    private HeightCurvePath heightCurvePath;
    private boolean isAboveWater = true;

    public HeightCurveElement(double[] outerPolygon, float height, double[] bounds) {
        super(bounds);
        this.outerPolygon = outerPolygon;
        this.height = height;
        setAboveWater();
    }

    public static HeightCurveElement mapToHeightCurveElement(ParserHeightCurveElement parserHeightCurveElement) {
        return new HeightCurveElement(
                forceCounterClockwise(parserHeightCurveElement.getCoordinates()),
                parserHeightCurveElement.getHeight(),
                parserHeightCurveElement.calculateBounds()
        );
    }

    public boolean getIsAboveWater() {
        return isAboveWater;
    }
    public double[] getCoordinates() {
        return outerPolygon;
    }
    public float getHeight() {
        return height;
    }

    public void addInnerPolygon(double[] innerPolygon) {
        innerPolygons.add(innerPolygon);
        heightCurvePath = null;
    }
    public void removeInnerPolygon(double[] innerPolygon) {
        innerPolygons.remove(innerPolygon);
        heightCurvePath = null;
    }
    public void removeAllInnerPolygons() {
        innerPolygons.clear();
        heightCurvePath = null;
    }

    public void setAboveWater() {
        isAboveWater = true;
        setStyle(STYLE_ABOVE_WATER);
    }
    public void setBelowWater() {
        isAboveWater = false;
        setStyle(STYLE_BELOW_WATER);
    }
    public void setSelected() {
        setStyle(STYLE_SELECTED);
    }
    public void setUnselected() {
        setStyle(isAboveWater ? STYLE_ABOVE_WATER : STYLE_BELOW_WATER);
    }

    public boolean contains(HeightCurveElement other) {
        if (super.containsBoundingBox(other)) {
            return PolygonUtils.contains(this.outerPolygon, other.outerPolygon);
        } else {
            return false;
        }
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (getColor() == null) setAboveWater();
        if (heightCurvePath == null) {
            this.heightCurvePath = new HeightCurvePath(outerPolygon, innerPolygons);
        }

        g2d.setColor(getColor());
        if (getStroke() == null) {
            g2d.fill(heightCurvePath);
        } else {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(heightCurvePath);
        }
    }
}