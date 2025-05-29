package dk.itu.data.models.parser;

import java.util.ArrayList;
import java.util.List;

public class ParserHeightCurveElement {
    private final List<Long> gmlIds = new ArrayList<>();
    private float[] coordinates;
    private final float height;

    public ParserHeightCurveElement(
            long gmlId,
            float[] coordinates,
            float height
    ) {
        this.gmlIds.add(gmlId);
        this.coordinates = coordinates;
        this.height = height;
    }

    public List<Long> getGmlIds() {
        return gmlIds;
    }
    public float[] getCoordinates() {
        return coordinates;
    }
    public float getHeight() {
        return height;
    }

    public void addGmlIds(List<Long> gmlIds) {
        this.gmlIds.addAll(gmlIds);
    }
    public void setCoordinates(float[] coordinates) {
        this.coordinates = coordinates;
    }
}