package dk.itu.drawing.models;

import dk.itu.models.OsmElement;

import java.util.List;

public class MapModelOsmFile extends MapModel {
    public MapModelOsmFile(float minX, float minY, float maxY, List<List<OsmElement>> layers) {
        super();
        this.minX = minX;
        this.minY = minY;
        this.maxY = maxY;
        this.layers = layers;
    }
}
