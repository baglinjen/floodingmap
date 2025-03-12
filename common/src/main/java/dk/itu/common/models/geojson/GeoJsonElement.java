package dk.itu.common.models.geojson;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Drawable;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.List;

import static dk.itu.util.DrawingUtils.toARGB;

public class GeoJsonElement extends Drawable {
    public static final DrawingConfiguration.Style styleAboveWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#ff450020")), true), 1);
    public static final DrawingConfiguration.Style styleBelowWater = new DrawingConfiguration.Style(new Color(toARGB(javafx.scene.paint.Color.web("#40739e50")), true), 1);
    private final float height;
    private final Shape shape;
    private double absoluteArea;

    public GeoJsonElement(float height, Path2D path) {
        this.height = height;
        this.shape = path;
        setShouldBeDrawn(true);
        calculateAbsoluteArea();
    }

    public float getHeight() {
        return height;
    }

    public GeoJsonElement updateStyle(float waterLevel) {
        setStyle(waterLevel > height ? styleBelowWater : styleAboveWater);
        return this;
    }

    public Shape getShape() {
        return shape;
    }

    private void calculateAbsoluteArea() {
        PathIterator iterator = shape.getPathIterator(null);
        double area = 0;
        double[] coords = new double[6];
        double startX = 0, startY = 0;
        double prevX = 0, prevY = 0;
//        boolean first = true;

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
        var rgba = getRgbaColor();
        if (rgba != null) {
            g2d.setColor(getRgbaColor());
            g2d.fill(shape);
        }
    }
}
