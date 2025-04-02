package dk.itu.data.models.db;

import java.awt.*;
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

    public double area() {
        return (maxLon - minLon) * (maxLat - minLat);
    }

    public BoundingBox getExpanded(BoundingBox other) {
        return new BoundingBox(
                Math.min(this.minLon, other.minLon),
                Math.min(this.minLat, other.minLat),
                Math.max(this.maxLon, other.maxLon),
                Math.max(this.maxLat, other.maxLat)
        );
    }

    public double distanceTo(Point p) {
        double dx = Math.max(0, Math.max(minLon - p.x, p.x - maxLon));
        double dy = Math.max(0, Math.max(minLat - p.y, p.y - maxLat));
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
