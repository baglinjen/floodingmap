package dk.itu.data.models.db;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class BoundingBox implements Serializable {
    private double minLon, minLat, maxLon, maxLat;

    public BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
    }

    public void expand(BoundingBox other) {
        minLon = Math.min(this.minLon, other.minLon);
        minLat = Math.min(this.minLat, other.minLat);
        maxLon = Math.max(this.maxLon, other.maxLon);
        maxLat = Math.max(this.maxLat, other.maxLat);
    }

    public boolean intersects(BoundingBox other) {
        return !(this.minLon > other.maxLon || this.maxLon < other.minLon ||
                this.minLat > other.maxLat || this.maxLat < other.minLat);
    }
    public boolean contains(Point2D.Double point) {
        return point.x >= minLon && point.x <= maxLon && point.y >= minLat && point.y <= maxLat;
    }

    public double intersectionArea(BoundingBox other) {
        if (!intersects(other)) return 0;

        var difLon = Math.max(Math.abs(minLon - other.maxLon), Math.abs(maxLon - other.minLon));
        var difLat = Math.max(Math.abs(minLat - other.maxLat), Math.abs(maxLat - other.minLat));

        return difLon * difLat;
    }

    public double area() {
        return (maxLon - minLon) * (maxLat - minLat);
    }
    public double minimumLength() {
        var width = maxLon - minLon;
        var height = maxLat - minLat;
        return Math.min(width, height);
    }

    public BoundingBox getExpanded(BoundingBox other) {
        return new BoundingBox(
                Math.min(this.minLon, other.minLon),
                Math.min(this.minLat, other.minLat),
                Math.max(this.maxLon, other.maxLon),
                Math.max(this.maxLat, other.maxLat)
        );
    }

    public double distanceTo(Point2D.Double p) {
        double bestDx = Math.min(Math.abs(minLon - p.x), Math.abs(maxLon - p.x));
        double bestDy = Math.min(Math.abs(minLat - p.y), Math.abs(maxLat - p.y));
        return Math.sqrt(Math.pow(bestDx, 2) + Math.pow(bestDy, 2));
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
}
