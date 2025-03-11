package dk.itu.data.repositories;

import dk.itu.common.models.geojson.GeoJsonElement;

import java.util.Arrays;
import java.util.List;

public class GeoJsonElementRepository {
    @SafeVarargs
    public final void add(List<GeoJsonElement>... geoJsonElements) {
        Arrays.stream(geoJsonElements).parallel().forEach(this::add);
    }
    public void add(List<GeoJsonElement> geoJsonElements) {
        // TODO: Add OSM Elements
        throw new UnsupportedOperationException();
    }

    public boolean connectionEstablished() {
        return false;
    }

    public boolean areElementsInDatabase() {
        return false;
    }
}