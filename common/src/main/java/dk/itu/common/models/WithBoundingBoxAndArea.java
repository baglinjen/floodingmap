package dk.itu.common.models;

public interface WithBoundingBoxAndArea extends WithBoundingBox {
    float getArea();

    static float calculateArea(float minLon, float minLat, float maxLon, float maxLat) {
        return (maxLon - minLon) * (maxLat - minLat);
    }
}
