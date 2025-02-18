package dk.itu.drawing.models;

import dk.itu.models.OsmElement;

import java.util.List;

public class MapModelDb extends MapModel {
    public MapModelDb(double minLon, double minLat, double maxLat, List<List<OsmElement>> layers) {
        super();
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.layers = layers;
    }
}
