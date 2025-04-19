package dk.itu.common.configurations;

import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public class CommonConfiguration {
    private static final Logger logger = LoggerFactory.getLogger();
    private static CommonConfiguration commonConfiguration = null;
    public static CommonConfiguration getInstance() {
        if (commonConfiguration == null) {
            commonConfiguration = new CommonConfiguration();
        }
        return commonConfiguration;
    }

    private final boolean forceParseOsm, forceParseGeoJson;
    private final String dataForsyningenToken;
    private final boolean useDb;

    private CommonConfiguration() {
        this.forceParseOsm = Boolean.parseBoolean(Objects.requireNonNullElse(System.getenv("forceParseOsm"), "false"));
        this.forceParseGeoJson = Boolean.parseBoolean(Objects.requireNonNullElse(System.getenv("forceParseGeoJson"), "false"));
        this.dataForsyningenToken = System.getenv("dataForsyningenToken");
        this.useDb = Boolean.parseBoolean(Objects.requireNonNullElse(System.getenv("useDb"), "false"));
    }

    public boolean shouldForceParseOsm() {
        return this.forceParseOsm;
    }

    public boolean shouldForceParseGeoJson() {
        return this.forceParseGeoJson;
    }

    public String getDataForsyningenToken() {
        return this.dataForsyningenToken;
    }
    public boolean getUseDb() {
        return this.useDb;
    }

    public List<String> getDataFiles() {
        try {
            List<String> dataFiles = new ArrayList<>();
            dataFiles.addAll(
                    Arrays.stream(Objects.requireNonNull(
                                    new File(
                                            CommonConfiguration.class
                                                    .getClassLoader()
                                                    .getResources("osm")
                                                    .nextElement()
                                                    .toURI()
                                    ).listFiles()
                            ))
                            .parallel()
                            .map(File::getName)
                            .toList()
            );
            dataFiles.addAll(
                    Arrays.stream(Objects.requireNonNull(
                                    new File(
                                            CommonConfiguration.class
                                                    .getClassLoader()
                                                    .getResources("gml")
                                                    .nextElement()
                                                    .toURI()
                                    ).listFiles()
                            ))
                            .parallel()
                            .map(File::getName)
                            .toList()
            );
            return dataFiles;
        } catch (Exception e) {
            logger.warn("Can't find resources: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public SqlCredentials getSqlCredentials() {
        return new SqlCredentials("jdbc:postgresql://localhost:5433/postgres", "postgres", "password");
    }

    public record SqlCredentials(String url, String username, String password) {}
}
