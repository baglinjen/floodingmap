package dk.itu;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.dto.GeoJsonParserResult;
import dk.itu.data.parsers.GeoJsonParser;
import dk.itu.data.parsers.OsmParser;
import dk.itu.data.dto.OsmParserResult;
import dk.itu.ui.RunningApplication;

public class Main {
    private static Services services;
    public static void main(String[] args) {
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
        if (!CommonConfiguration.getInstance().shouldForceParseOsm()) {
            return;
        }

        // Prepare result
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse("fyn.osm", osmParserResult);

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