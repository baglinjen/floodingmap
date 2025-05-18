package dk.itu.data.models.parser;

import java.util.ArrayList;
import java.util.List;

public class ParserHeightCurveElement {
    private final List<Long> gmlIds = new ArrayList<>();
    private double[] coordinates;
    private final float height;

    public ParserHeightCurveElement(
            long gmlId,
            double[] coordinates,
            float height
    ) {
        this.gmlIds.add(gmlId);
        this.coordinates = coordinates;
        this.height = height;
    }

    public List<Long> getGmlIds() {
        return gmlIds;
    }
    public double[] getCoordinates() {
        return coordinates;
    }
    public float getHeight() {
        return height;
    }

    public void addGmlIds(List<Long> gmlIds) {
        this.gmlIds.addAll(gmlIds);
    }
    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public double[] calculateBounds() {
        double minLon = Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double maxLat = Double.MIN_VALUE;
        for (int i = 0; i < coordinates.length; i+=2) {
            double lon = coordinates[i];
            double lat = coordinates[i+1];

            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
        }

        return new double[]{minLon, minLat, maxLon, maxLat};
    }
}