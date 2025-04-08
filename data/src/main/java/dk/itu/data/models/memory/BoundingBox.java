package dk.itu.data.models.memory;

import java.awt.*;

public class BoundingBox {
    private double minX, minY, maxX, maxY;

    public BoundingBox(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void expand(BoundingBox other) {
        minX = Math.min(this.minX, other.minX);
        minY = Math.min(this.minY, other.minY);
        maxX = Math.max(this.maxX, other.maxX);
        maxY = Math.max(this.maxY, other.maxY);
    }

    public double intersectionArea(BoundingBox other) {
        double ixMin = Math.max(this.minX, other.minX);
        double iyMin = Math.max(this.minY, other.minY);
        double ixMax = Math.min(this.maxX, other.maxX);
        double iyMax = Math.min(this.maxY, other.maxY);

        double width = ixMax - ixMin;
        double height = iyMax - iyMin;

        if (width <= 0 || height <= 0) {
            return 0.0;
        }

        return width * height;
    }

    public boolean intersects(BoundingBox other) {
        return !(this.minX > other.maxX || this.maxX < other.minX ||
                this.minY > other.maxY || this.maxY < other.minY);
    }

    public double area() {
        return (maxX - minX) * (maxY - minY);
    }

    public BoundingBox getExpanded(BoundingBox other) {
        return new BoundingBox(
                Math.min(this.minX, other.minX),
                Math.min(this.minY, other.minY),
                Math.max(this.maxX, other.maxX),
                Math.max(this.maxY, other.maxY)
        );
    }

    public double distanceFromPoint(Point p) {
        double dx = Math.max(0, Math.max(minX - p.x, p.x - maxX));
        double dy = Math.max(0, Math.max(minY - p.y, p.y - maxY));
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceToBoundingBox(BoundingBox other) {
        double dx = Math.max(0, Math.max(
                        other.getMinX() - this.getMaxX(),
                        this.getMinX() - other.getMaxX()
                ));

        double dy = Math.max(0, Math.max(
                        other.getMinY() - this.getMaxY(),
                        this.getMinX() - other.getMaxX()
                ));

        return Math.sqrt(dx * dx + dy * dy);
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }
}
