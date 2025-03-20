package dk.itu.common.configurations;

import java.util.Properties;

public class CommonConfiguration {
    private static CommonConfiguration commonConfiguration = null;
    public static CommonConfiguration getInstance() {
        if (commonConfiguration == null) {
            commonConfiguration = new CommonConfiguration();
        }
        return commonConfiguration;
    }

    private final boolean forceParseOsm, forceParseGeoJson;

    private CommonConfiguration() {
        Properties properties = System.getProperties();
        this.forceParseOsm = Boolean.parseBoolean(properties.getProperty("configuration.forceParseOsm", "false"));
        this.forceParseGeoJson = Boolean.parseBoolean(properties.getProperty("configuration.forceParseGeoJson", "false"));
    }

    public boolean shouldForceParseOsm() {
        return this.forceParseOsm;
    }

    public boolean shouldForceParseGeoJson() {
        return this.forceParseGeoJson;
    }

    public SqlCredentials getSqlCredentials() {
        return new SqlCredentials("jdbc:postgresql://localhost:5433/postgres", "postgres", "password");
    }

    public record SqlCredentials(String url, String username, String password) {}
}
