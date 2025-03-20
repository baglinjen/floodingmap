package dk.itu.data.services;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.datastructure.curvetree.CurveTree;
import dk.itu.data.dto.GeoJsonParserResult;
import dk.itu.data.models.parser.ParserGeoJsonElement;
import dk.itu.data.parsers.GeoJsonParser;
import dk.itu.data.repositories.GeoJsonElementRepository;
import dk.itu.data.repositories.GeoJsonElementRepositoryDb;
import dk.itu.data.repositories.GeoJsonElementRepositoryMemory;

import java.sql.Connection;
import java.util.List;

public class GeoJsonService {
    private final GeoJsonElementRepository geoJsonElementRepository;

    public GeoJsonService() {
        geoJsonElementRepository = new GeoJsonElementRepositoryMemory();
    }

    public GeoJsonService(Connection connection) {
        geoJsonElementRepository = new GeoJsonElementRepositoryDb(connection);
    }

    public List<GeoJsonElement> getGeoJsonElements() {
        return geoJsonElementRepository.getGeoJsonElements();
    }

    public void loadGeoJsonData(String geoJsonFileName) {
        GeoJsonParserResult geoJsonParserResult = new GeoJsonParserResult();

        // Get data from GeoJson file
        GeoJsonParser.parse(geoJsonFileName, geoJsonParserResult);

        // Filter and sort data
        geoJsonParserResult.sanitize();

        // Create data structure
        createDataStructure(geoJsonParserResult.getGeoJsonElements());

        geoJsonElementRepository.add(geoJsonParserResult.getGeoJsonElements());
    }

    private void createDataStructure(List<ParserGeoJsonElement> geoJsonElements) {
        var curveTree = new CurveTree();
        for (ParserGeoJsonElement geoJsonElement : geoJsonElements) {
            curveTree.put(geoJsonElement);
        }
    }

    public float getMinWaterLevel() {
        return geoJsonElementRepository.getMinWaterLevel();
    }

    public float getMaxWaterLevel() {
        return geoJsonElementRepository.getMaxWaterLevel();
    }
}
