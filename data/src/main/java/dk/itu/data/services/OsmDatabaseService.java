package dk.itu.data.services;

import dk.itu.common.models.osm.OsmElement;
import dk.itu.data.repositories.OsmElementRepository;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.io.ParseException;

import java.util.ArrayList;
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

    private OsmDatabaseService() {
    }

    public boolean areElementsInDatabase() {
        try (OsmElementRepository repository = new OsmElementRepository()) {
            return repository.areElementsInDatabase();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertOsmElementsInDb(List<OsmElement> osmElement) {
        try (OsmElementRepository repository = new OsmElementRepository()) {
            repository.add(osmElement);
            return true;
        } catch (Exception e) {
            logger.warn("Couldn't connect to the database, using in-memory solution:\n{}", e.getMessage());
            return false;
        }
    }

    public List<OsmElement> fetchAllOsmElements() throws ParseException {
        try (OsmElementRepository repository = new OsmElementRepository()) {
            return repository.getOsmElements();
        } catch (Exception e) {
            logger.warn("Couldn't connect to the database:\n{}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
