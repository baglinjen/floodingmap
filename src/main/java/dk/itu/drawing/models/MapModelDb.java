package dk.itu.drawing.models;

import dk.itu.models.OsmElement;

import java.util.List;

public class MapModelDb extends MapModel {
    public MapModelDb(double minLon, double minLat, double maxLat, double maxLon, List<OsmElement> areaElements, List<OsmElement> pathElements) {
        super();
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.maxLon = maxLon;

        this.sortAndSplitLayers(areaElements, pathElements);
    }
}
