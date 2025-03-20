package dk.itu.data.services;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface Services {
    OsmService getOsmService();
    OsmService getTempOsmService();

    Logger logger = LoggerFactory.getLogger();
    private static Connection getConnection() {
        try {
            var credentials = CommonConfiguration.getInstance().getSqlCredentials();
            return DriverManager.getConnection(credentials.url(), credentials.username(), credentials.password());
        } catch (SQLException e) {
            logger.error("Failed to create connection:\n{}", e.getMessage());
            return null;
        }
    }
    static Services withServices(Consumer<Services> consumer) {
        consumer.accept(new Services() {

            private final OsmService osmService = new OsmService(Services.getConnection());

            @Override
            public OsmService getOsmService() {
                return osmService;
            }

            @Override
            public OsmService getTempOsmService() {
                return new OsmService(Services.getConnection());
            }
        });
    }
}
