package dk.itu.data.models.parser;

import dk.itu.common.models.GeoJsonElement;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.PolygonUtils.*;

public class ParserGeoJsonElement extends ParserDrawable implements GeoJsonElement {
    private final float height;
    private final double[] outerPolygon;
    private final List<double[]> innerPolygons = new ArrayList<>();
    private final double[] bounds = new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
    private Path2D.Double shape;
    private final double area;
    private double absoluteArea;
    private boolean belowWater;
    private boolean selected;

    public ParserGeoJsonElement(float height, double[] outerPolygon) {
        this.height = height;
        this.outerPolygon = forceCounterClockwise(outerPolygon);
        this.area = calculatePolygonArea(outerPolygon);

        for (int i = 0; i < outerPolygon.length; i+=2) {
            if (outerPolygon[i] < bounds[0]) bounds[0] = outerPolygon[i];
            if (outerPolygon[i] > bounds[2]) bounds[2] = outerPolygon[i];
            if (outerPolygon[i+1] < bounds[1]) bounds[1] = outerPolygon[i+1];
            if (outerPolygon[i+1] > bounds[3]) bounds[3] = outerPolygon[i+1];
        }

        this.belowWater = false;
        setShouldBeDrawn(true);
        setStyle(styleBelowWater);
    }

    public double[] getBounds() {
        return bounds;
    }

    public List<double[]> getInnerPolygons() {
        return innerPolygons;
    }

    public void addInnerPolygon(double[] innerPolygon) {
        innerPolygons.add(forceClockwise(innerPolygon));
    }

    public void calculateShape() {
        this.shape = pathFromPolygonLists(List.of(outerPolygon), innerPolygons);
    }

    public float getHeight() {
        return height;
    }

    public double[] getOuterPolygon() {
        return outerPolygon;
    }

    public double getArea() {
        return area;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean contains(ParserGeoJsonElement parserGeoJsonElement) {
        return contains(this.outerPolygon, parserGeoJsonElement);
    }

    public static boolean contains(double[] p1, ParserGeoJsonElement parserGeoJsonElement) {
        var p = parserGeoJsonElement.outerPolygon;
        var isInside = true;
        var i = 2;
        while (isInside && i < p.length) {
            isInside = isPointInPolygon(p1, p[i], p[i+1]);
            i+=2;
        }
        return isInside;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        setStyle(styleSelected);
        var rgba = (belowWater || selected) ? getRgbaColor() : Color.BLACK;

        if(rgba != null){
            g2d.setColor(rgba);
            if (belowWater || selected) {
                g2d.fill(shape);
            } else {
                g2d.draw(shape);
            }
        }
    }

    @Override
    public void setBelowWater(boolean belowWater){
        this.belowWater = belowWater;
    }
}
