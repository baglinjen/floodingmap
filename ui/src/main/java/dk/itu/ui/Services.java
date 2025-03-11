package dk.itu.ui;

import dk.itu.data.services.GeoJsonService;
import dk.itu.data.services.OsmService;

public class Services {
    public OsmService osmService;
    public GeoJsonService geoJsonService;
    public Services() {
        osmService = OsmService.getInstance();
        geoJsonService = GeoJsonService.getInstance();
    }
}
