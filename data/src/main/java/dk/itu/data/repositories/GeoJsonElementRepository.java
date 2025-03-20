package dk.itu.data.repositories;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.List;

public interface GeoJsonElementRepository {
    void add(List<ParserGeoJsonElement> geoJsonElements);
    List<GeoJsonElement> getGeoJsonElements();
    float getMinWaterLevel();
    float getMaxWaterLevel();
}