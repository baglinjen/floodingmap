package dk.itu.data.repositories;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonElementRepositoryMemory implements GeoJsonElementRepository {
    private final List<GeoJsonElement> geoJsonElements = new ArrayList<>();
    @Override
    public void add(List<ParserGeoJsonElement> geoJsonElements) {
        this.geoJsonElements.addAll(geoJsonElements);
    }

    @Override
    public List<GeoJsonElement> getGeoJsonElements() {
        return geoJsonElements;
    }

    @Override
    public float getMinWaterLevel() {
        return 0;
    }

    @Override
    public float getMaxWaterLevel() {
        return 15;
    }
}
