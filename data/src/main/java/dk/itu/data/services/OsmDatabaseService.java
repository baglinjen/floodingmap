package dk.itu.data.services;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OsmDatabaseService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger();

    private OsmElementRepository repository;

    public OsmDatabaseService() {
        try {
            this.repository = new OsmElementRepository();
        } catch (Exception e) {
            logger.warn("Couldn't connect to the database.\n{}", e.getMessage());
            this.repository = null;
        }
    }

    public boolean areElementsInDatabase() {
        if (repository == null) {
            return false;
        } else {
            return repository.areElementsInDatabase();
        }
    }

    public boolean insertOsmElementsInDb(List<ParserOsmElement> osmElements) {
        if (repository == null) {
            return false;
        } else {
            repository.add(osmElements);
            return true;
        }
    }

    public List<OsmElement> fetchAllOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        if (repository == null) {
            return new ArrayList<>();
        } else {
            return repository.getOsmElements(limit, minLon, minLat, maxLon, maxLat);
        }
    }

    @Override
    public void close() throws SQLException {
        if (this.repository != null) {
            this.repository.close();
        }
    }
}
