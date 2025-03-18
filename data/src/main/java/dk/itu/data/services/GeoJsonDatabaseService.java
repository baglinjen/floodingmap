package dk.itu.data.services;

import dk.itu.common.models.GeoJsonElement;
import dk.itu.data.models.parser.ParserGeoJsonElement;
import dk.itu.data.repositories.GeoJsonElementRepository;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class GeoJsonDatabaseService {
    private static final Logger logger = LoggerFactory.getLogger();
    private static GeoJsonDatabaseService instance;

    public static GeoJsonDatabaseService getInstance() {
        if (instance == null) {
            instance = new GeoJsonDatabaseService();
        }
        return instance;
    }

    private final GeoJsonElementRepository geoJsonElementRepository;

    private GeoJsonDatabaseService() {
        geoJsonElementRepository = new GeoJsonElementRepository();
    }

    public boolean connectionEstablished() {
        return geoJsonElementRepository.connectionEstablished();
    }

    public boolean areElementsInDatabase() {
        return geoJsonElementRepository.areElementsInDatabase();
    }

    public List<GeoJsonElement> getGeoJsonElements() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void insertGeoJsonElementsInDb(List<ParserGeoJsonElement> geoJsonElements) {
        if (geoJsonElementRepository.connectionEstablished()) {
            geoJsonElementRepository.add(geoJsonElements);
        } else {
            logger.warn("Couldn't connect to the database, using in-memory solution");
        }
    }
}
