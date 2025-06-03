package dk.itu.common.models;

public interface WithBoundingBoxAndArea extends WithBoundingBox {
    double getArea();

    static double calculateArea(double minLon, double minLat, double maxLon, double maxLat) {
        return (maxLon - minLon) * (maxLat - minLat);
    }
}
