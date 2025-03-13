package dk.itu;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.GeoJsonParserResult;
import dk.itu.data.parsers.GeoJsonParser;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.ui.RunningApplication;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    private static Services services;
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5433/postgres";
        String user = "postgres";
        String password = "password";

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);

            // Print test to confirm db connection
            var result = context.selectFrom("nodes").fetch();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        init();
        RunningApplication.main(args);
    }

    private static void init() {
        services = new Services();

        loadOsmDataInDb();
        loadGeoJsonDataInDb();
    }

    private static void loadOsmDataInDb() {
        // If already in DB and !CommonConfiguration.getInstance().shouldForceParseOsm() => Jump over
        if (!CommonConfiguration.getInstance().shouldForceParseOsm() && services.osmService.osmDatabaseService.areElementsInDatabase()) {
            return;
        }

        // Prepare result
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse("modified-tuna.osm", osmParserResult);

        // Filter and sort data
        osmParserResult.sanitize();

        // Add to Database
        services.osmService.addOsmParserResultInDatabase(osmParserResult);
    }

    private static void loadGeoJsonDataInDb() {
        // If already in DB and !CommonConfiguration.getInstance().shouldForceParseGeoJson() => Jump over
        if (!CommonConfiguration.getInstance().shouldForceParseGeoJson() && services.geoJsonService.geoJsonDatabaseService.areElementsInDatabase()) {
            return;
        }

        GeoJsonParserResult geoJsonParserResult = new GeoJsonParserResult();

        // Get data from GeoJson file
        GeoJsonParser.parse("modified-tuna.geojson", geoJsonParserResult);

        // Filter and sort data
        geoJsonParserResult.sanitize();

        services.geoJsonService.addGeoJsonParserResultInDatabase(geoJsonParserResult);
    }
}