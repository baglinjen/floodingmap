package dk.itu.data.models.parser;

import dk.itu.common.models.GeoJsonElement;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

public class ParserGeoJsonElement extends ParserDrawable implements GeoJsonElement {
    private final float height;
    private final double[] coordinates;
    private final Shape shape;
    private double absoluteArea;
    private boolean belowWater;

    public ParserGeoJsonElement(float height, double[] coordinates, Path2D path) {
        this.height = height;
        this.shape = new Area(path);
        this.coordinates = coordinates;
        this.belowWater = false;
        setShouldBeDrawn(true);
        setStyle(styleBelowWater);
        calculateAbsoluteArea();
    }

    public float getHeight() {
        return height;
    }

    public Shape getShape() {
        return shape;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    private void calculateAbsoluteArea() {
        PathIterator iterator = shape.getPathIterator(null);
        double area = 0;
        double[] coords = new double[6];
        double startX = 0, startY = 0;
        double prevX = 0, prevY = 0;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            double x = coords[0], y = coords[1];

            if (type == PathIterator.SEG_MOVETO) {
                startX = x;
                startY = y;
                prevX = x;
                prevY = y;
            } else if (type == PathIterator.SEG_LINETO) {
                area += (prevX * y - x * prevY);
                prevX = x;
                prevY = y;
            } else if (type == PathIterator.SEG_CLOSE) {
                area += (prevX * startY - startX * prevY);
            }
            iterator.next();
        }
        absoluteArea = Math.abs(area) / 2.0;
    }

    public double getAbsoluteArea() {
        return absoluteArea;
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
