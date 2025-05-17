package dk.itu.data.models;

import dk.itu.common.configurations.DrawingConfiguration;
import dk.itu.common.models.Colored;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

public abstract class BoundingBox extends Colored implements Serializable {
    public static final DrawingConfiguration.Style STYLE = new DrawingConfiguration.Style(Color.BLACK, 1);
    private double minLon, minLat, maxLon, maxLat, area;

    public BoundingBox() {}

    public BoundingBox(double[] boundingBox) {
        this.minLon = boundingBox[0];
        this.minLat = boundingBox[1];
        this.maxLon = boundingBox[2];
        this.maxLat = boundingBox[3];
        calculateArea();
    }

    public BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        calculateArea();
    }

    public void setBoundingBox(BoundingBox bbox) {
        this.minLon = bbox.getMinLon();
        this.minLat = bbox.getMinLat();
        this.maxLon = bbox.getMaxLon();
        this.maxLat = bbox.getMaxLat();
        this.area = bbox.getArea();
    }
    public void setBoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        calculateArea();
    }

    public double[] getBoundingBoxWithArea() {
        return new double[]{minLon, minLat, maxLon, maxLat, area};
    }

    public void expand(BoundingBox other) {
        minLon = Math.min(this.minLon, other.minLon);
        minLat = Math.min(this.minLat, other.minLat);
        maxLon = Math.max(this.maxLon, other.maxLon);
        maxLat = Math.max(this.maxLat, other.maxLat);
        calculateArea();
    }
    public static void expand(double[] a, BoundingBox b) {
        expand(a, b.getBoundingBoxWithArea());
    }
    public static void expand(double[] a, double[] b) {
        a[0] = Math.min(a[0], b[0]);
        a[1] = Math.min(a[1], b[1]);
        a[2] = Math.max(a[2], b[2]);
        a[3] = Math.max(a[3], b[3]);
        a[4] = calculateArea(a);
    }


    public boolean intersects(BoundingBox other) {
        return !(this.minLon > other.maxLon || this.maxLon < other.minLon ||
                this.minLat > other.maxLat || this.maxLat < other.minLat);
    }
    public boolean intersects(double[] other) {
        return !(this.minLon > other[2] || this.maxLon < other[0] ||
                this.minLat > other[3] || this.maxLat < other[1]);
    }
    public static boolean intersects(double[] a, double[] other) {
        return !(a[0] > other[2] || a[2] < other[0] ||
                a[1] > other[3] || a[3] < other[1]);
    }

    public static double intersectionArea(double[] bb1, double[] bb2) {
        if (!intersects(bb1, bb2)) return 0;

        return
                Math.max(Math.abs(bb1[0] - bb2[2]), Math.abs(bb1[2] - bb2[0]))
                *
                Math.max(Math.abs(bb1[1] - bb2[3]), Math.abs(bb1[3] - bb2[1]));
    }

    public double getArea() {
        return this.area;
    }
    private void calculateArea() {
        this.area = calculateArea(this.minLon, this.minLat, this.maxLon, this.maxLat);
    }
    private static double calculateArea(double[] a) {
        return calculateArea(a[0], a[1], a[2], a[3]);
    }
    private static double calculateArea(double minLon, double minLat, double maxLon, double maxLat) {
        return (maxLon - minLon) * (maxLat - minLat);
    }

    public double getEnlargementArea(BoundingBox other) {
        return calculateArea(
                Math.min(this.minLon, other.minLon),
                Math.min(this.minLat, other.minLat),
                Math.max(this.maxLon, other.maxLon),
                Math.max(this.maxLat, other.maxLat)
        );
    }

    public boolean containsBoundingBox(BoundingBox other) {
        return
                minLon <= other.minLon &&
                minLat <= other.minLat &&
                maxLon >= other.maxLon &&
                maxLat >= other.maxLat;
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