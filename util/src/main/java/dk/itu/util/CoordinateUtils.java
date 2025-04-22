package dk.itu.util;

import org.locationtech.proj4j.*;

public class CoordinateUtils {
    private static final CRSFactory crsFactory = new CRSFactory();
    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CoordinateReferenceSystem WGS84 = crsFactory.createFromName("epsg:4326"), UTM = crsFactory.createFromName("epsg:25832");
    private static final CoordinateTransform wgsToUtm = ctFactory.createTransform(WGS84, UTM), utmToWgs = ctFactory.createTransform(UTM, WGS84);

    public static double[] utmToWgs(double lon, double lat) {
        ProjCoordinate result = new ProjCoordinate();
        var transformed = utmToWgs.transform(new ProjCoordinate(lon, lat), result);
        return new double[] {transformed.x, transformed.y};
    }
    public static double[] wgsToUtm(double lon, double lat) {
        ProjCoordinate result = new ProjCoordinate();
        var transformed = wgsToUtm.transform(new ProjCoordinate(lon, lat), result);
        return new double[] {transformed.x, transformed.y};
    }

    /**
     * Calculate the haversine distance between two lon/lat points.
     *
     * @return distance in kilometers
     */
    public static double haversineDistance(double lon1, double lat1, double lon2, double lat2) {
        // Convert latitude and longitude from degrees to radians
        double latRad1 = Math.toRadians(lat1);
        double lonRad1 = Math.toRadians(lon1);
        double latRad2 = Math.toRadians(lat2);
        double lonRad2 = Math.toRadians(lon2);

        // Haversine formula
        double dLat = latRad2 - latRad1;
        double dLon = lonRad2 - lonRad1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(latRad1) * Math.cos(latRad2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371.0 * c;
    }
}