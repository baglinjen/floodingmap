package dk.itu.data.models.parser;

import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.PolygonUtils.calculatePolygonArea;

public class ParserHeightCurveElement {
    private final List<Long> gmlIds = new ArrayList<>();
    private double[] coordinates;
    private final float height;
    private Double area = null;

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

    public double calculateArea() {
        if (area == null) {
            area = calculatePolygonArea(this.coordinates);
        }
        return area;
    }
}
