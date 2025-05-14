package dk.itu.data.models.db.heightcurve;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Colored;
import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.PolygonUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.DrawingUtils.toARGB;
import static dk.itu.util.PolygonUtils.forceCounterClockwise;
import static dk.itu.util.ShapePreparer.prepareComplexPolygon;

public class HeightCurveElement extends BoundingBox {
    private static final DrawingConfiguration.Style STYLE_ABOVE_WATER = new DrawingConfiguration.Style(Color.BLACK, 1);
    private static final DrawingConfiguration.Style STYLE_BELOW_WATER = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e80")), true), null);
    private static final DrawingConfiguration.Style STYLE_SELECTED = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#00FF0080")), true), null);
    private final double[] outerPolygon;
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final float height;
    private final double polygonArea;
    private Path2D.Double path;
    private boolean isAboveWater = true;

    public HeightCurveElement(double[] outerPolygon, float height, double polygonArea, double[] bounds) {
        super(bounds);
        this.outerPolygon = outerPolygon;
        this.height = height;
        this.polygonArea = polygonArea;
        setAboveWater();
    }

    public static HeightCurveElement mapToHeightCurveElement(ParserHeightCurveElement parserHeightCurveElement) {
        return new HeightCurveElement(
                forceCounterClockwise(parserHeightCurveElement.getCoordinates()),
                parserHeightCurveElement.getHeight(),
                parserHeightCurveElement.calculateArea(),
                parserHeightCurveElement.calculateBounds()
        );
    }

    public boolean getIsAboveWater() {
        return isAboveWater;
    }

    public double[] getCoordinates() {
        return outerPolygon;
    }
    public void addInnerPolygon(double[] innerPolygon) {
        innerPolygons.add(innerPolygon);
    }
    public void removeInnerPolygon(double[] innerPolygon) {
        innerPolygons.remove(innerPolygon);
    }
    public void removeAllInnerPolygons() {
        innerPolygons.clear();
    }
    public double getPolygonArea() {
        return polygonArea;
    }
    public float getHeight() {
        return height;
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
        if (super.contains(other)) {
            return PolygonUtils.contains(this.outerPolygon, other.outerPolygon);
        } else {
            return false;
        }
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {
        path = prepareComplexPolygon(g2d, List.of(outerPolygon), innerPolygons, DRAWING_TOLERANCE);
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        if (path == null) return;
        if (getColor() == null) setAboveWater();

        g2d.setColor(getColor());
        if (getStroke() == null) {
            g2d.fill(path);
        } else {
            g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
            g2d.draw(path);
        }
    }
}
