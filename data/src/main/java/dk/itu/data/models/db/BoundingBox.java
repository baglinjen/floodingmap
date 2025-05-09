package dk.itu.data.models.db;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Colored;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

public class BoundingBox extends Colored implements Serializable {
    private static final DrawingConfiguration.Style STYLE = new DrawingConfiguration.Style(Color.BLACK, 1);
    private double minLon, minLat, maxLon, maxLat, area;

    public BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        setStyle(STYLE);
        calculateArea();
    }

    public void expand(BoundingBox other) {
        minLon = Math.min(this.minLon, other.minLon);
        minLat = Math.min(this.minLat, other.minLat);
        maxLon = Math.max(this.maxLon, other.maxLon);
        maxLat = Math.max(this.maxLat, other.maxLat);
        calculateArea();
    }

    public boolean intersects(BoundingBox other) {
        return !(this.minLon > other.maxLon || this.maxLon < other.minLon ||
                this.minLat > other.maxLat || this.maxLat < other.minLat);
    }

    public double intersectionArea(BoundingBox other) {
        if (!intersects(other)) return 0;

        var difLon = Math.max(Math.abs(minLon - other.maxLon), Math.abs(maxLon - other.minLon));
        var difLat = Math.max(Math.abs(minLat - other.maxLat), Math.abs(maxLat - other.minLat));

        return difLon * difLat;
    }

    public double area() {
        return this.area;
    }
    private void calculateArea() {
        this.area = (maxLon - minLon) * (maxLat - minLat);
    }

    public BoundingBox getExpanded(BoundingBox other) {
        return new BoundingBox(
                Math.min(this.minLon, other.minLon),
                Math.min(this.minLat, other.minLat),
                Math.max(this.maxLon, other.maxLon),
                Math.max(this.maxLat, other.maxLat)
        );
    }

    public double distanceToBoundingBox(BoundingBox other) {
        double dx = Math.max(0, Math.max(
                other.getMinLon() - this.getMaxLon(),
                this.getMinLon() - other.getMaxLon()
        ));

        double dy = Math.max(0, Math.max(
                other.getMinLat() - this.getMaxLat(),
                this.getMinLat() - other.getMaxLat()
        ));

        return Math.sqrt(dx * dx + dy * dy);
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public double getMaxLat() {
        return maxLat;
    }

    @Override
    public void prepareDrawing(Graphics2D g2d) {

    }

    @Override
    public boolean shouldDraw() {
        return true;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {
        g2d.setColor(getRgbaColor());
        g2d.setStroke(new BasicStroke(strokeBaseWidth * getStroke()));
        g2d.draw(new Rectangle2D.Double(0.56*minLon, -maxLat, 0.56*(maxLon - minLon), maxLat - minLat));
    }
}
