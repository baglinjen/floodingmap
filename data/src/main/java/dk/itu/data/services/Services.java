package dk.itu.data.services;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface Services {
    OsmService getOsmService(boolean withDb);
    GeoJsonService getGeoJsonService();

    Logger logger = LoggerFactory.getLogger();
    private static Connection getConnection() {
        try {
            var credentials = CommonConfiguration.getInstance().getSqlCredentials();
            return DriverManager.getConnection(credentials.url(), credentials.username(), credentials.password());
        } catch (SQLException e) {
            logger.error("Failed to create connection:\n{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    static void withServices(Consumer<Services> consumer) {
        List<Connection> connections = new ArrayList<>();
        consumer.accept(new Services() {
            private OsmService osmServiceDb;
            private GeoJsonService geoJsonService = null;

            @Override
            public OsmService getOsmService(boolean withDb) {
                if (withDb) {
                    if (osmServiceDb == null) {
                        var connection = getConnection();
                        connections.add(connection);
                        osmServiceDb = new OsmService(connection);
                    }
                    return osmServiceDb;
                } else {
                    return new OsmService();
                }
            }

            @Override
            public GeoJsonService getGeoJsonService() {
                if (geoJsonService == null) {
//                    var connection = getConnection();
                    geoJsonService = GeoJsonService.getInstance();
//                    connections.add(connection);
                }
                return geoJsonService;
            }
        });

        try {
            for (Connection connection : connections) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close connection:\n{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
