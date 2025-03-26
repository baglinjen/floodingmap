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
    private Path2D.Double shape;
    private final double area;
    private double absoluteArea;
    private boolean belowWater;

    public ParserGeoJsonElement(float height, double[] outerPolygon) {
        this.height = height;
        this.outerPolygon = forceCounterClockwise(outerPolygon);
        this.area = calculatePolygonArea(outerPolygon);
        this.belowWater = false;
        setShouldBeDrawn(true);
        setStyle(styleBelowWater);
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
        var rgba = belowWater ? getRgbaColor() : Color.BLACK;

        if(rgba != null){
            g2d.setColor(rgba);
            if(belowWater) g2d.fill(shape);
            else g2d.draw(shape);
        }
    }

    @Override
    public void setBelowWater(boolean belowWater){
        this.belowWater = belowWater;
    }
}
