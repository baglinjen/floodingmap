package dk.itu.data.services;

import dk.itu.common.models.osm.OsmElement;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OsmDatabaseService {
    private static final Logger logger = LoggerFactory.getLogger();
    private static OsmDatabaseService instance;

    public static OsmDatabaseService getInstance() {
        if (instance == null) {
            instance = new OsmDatabaseService();
        }
        return instance;
    }

    private final OsmElementRepository osmElementRepository;

    private OsmDatabaseService() {
        this.osmElementRepository = new OsmElementRepository();
    }

    public boolean connectionEstablished() {
        return osmElementRepository.connectionEstablished();
    }

    public boolean areElementsInDatabase() {
        return osmElementRepository.areElementsInDatabase();
    }

    public void insertOsmElementsInDb(List<OsmElement> osmElement) {
        if (osmElementRepository.connectionEstablished()) {
            osmElementRepository.add(osmElement);
        } else {
            logger.warn("Couldn't connect to the database, using in-memory solution");
        }
    }

    public List<OsmElement> getOsmElements() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public double getMinLat() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public double getMinLon() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public double getMaxLat() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    public double getMaxLon() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
