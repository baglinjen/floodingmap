package dk.itu.util;

public class CoordinateConverter {
    // Constants for WGS84 ellipsoid
    private static final double SEMI_MAJOR_AXIS = 6378137.0;
    private static final double FLATTENING = 1.0 / 298.257223563;
    private static final double ECCENTRICITY_SQUARED = 2 * FLATTENING - FLATTENING * FLATTENING;

    /**
     * Convert UTM coordinates to Latitude and Longitude
     * @param easting UTM Easting coordinate
     * @param northing UTM Northing coordinate
     * @return Double array [latitude, longitude]
     */
    public static double[] convertUTMToLatLon(double easting, double northing) {
        // UTM Zone 32N specific parameters
        double k0 = 0.9996;  // Scale factor
        double e1 = (1 - Math.sqrt(1 - ECCENTRICITY_SQUARED)) / (1 + Math.sqrt(1 - ECCENTRICITY_SQUARED));
        double zoneNumber = 32;
        double zoneCentralMeridian = (zoneNumber - 1) * 6 - 180 + 3;  // Central meridian for zone 32

        // Remove false easting and northing
        double x = easting - 500000.0;
        double y = northing;

        // Calculate Latitude
        double m = y / k0;
        double mu = m / (SEMI_MAJOR_AXIS * (1 - ECCENTRICITY_SQUARED / 4.0 -
                3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 64.0 -
                5 * Math.pow(ECCENTRICITY_SQUARED, 3) / 256.0));

        double phi1 = mu + (3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32.0) * Math.sin(2 * mu) +
                (21 * e1 * e1 / 16 - 55 * Math.pow(e1, 4) / 32.0) * Math.sin(4 * mu) +
                (151 * Math.pow(e1, 3) / 96.0) * Math.sin(6 * mu);

        double n1 = SEMI_MAJOR_AXIS / Math.sqrt(1 - ECCENTRICITY_SQUARED * Math.sin(phi1) * Math.sin(phi1));
        double t1 = Math.tan(phi1) * Math.tan(phi1);
        double c1 = ECCENTRICITY_SQUARED * Math.cos(phi1) * Math.cos(phi1) / (1 - ECCENTRICITY_SQUARED);
        double r1 = SEMI_MAJOR_AXIS * (1 - ECCENTRICITY_SQUARED) /
                Math.pow(1 - ECCENTRICITY_SQUARED * Math.sin(phi1) * Math.sin(phi1), 1.5);

        double d = x / (n1 * k0);

        // Latitude calculation
        double latitude = phi1 - (n1 * Math.tan(phi1) / r1) *
                (d * d / 2 - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * c1) *
                        Math.pow(d, 4) / 24 + (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 -
                        252 * c1 - 3 * c1 * c1) * Math.pow(d, 6) / 720);

        // Longitude calculation
        double longitude = zoneCentralMeridian / 180 * Math.PI +
                (d - (1 + 2 * t1 + c1) * Math.pow(d, 3) / 6 +
                        (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * c1 + 24 * t1 * t1) *
                                Math.pow(d, 5) / 120) / Math.cos(phi1);

        // Convert to degrees
        return new double[]{
                Math.toDegrees(latitude),
                Math.toDegrees(longitude)
        };
    }

    /**
     * Convert Latitude and Longitude to UTM coordinates (EPSG:25832)
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return Double array [easting, northing]
     */
    public static double[] convertLatLonToUTM(double latitude, double longitude) {
        // UTM Zone 32N specific parameters
        double k0 = 0.9996;  // Scale factor
        double zoneNumber = 32;
        double zoneCentralMeridian = (zoneNumber - 1) * 6 - 180 + 3;  // Central meridian for zone 32

        // Convert degrees to radians
        double phi = Math.toRadians(latitude);
        double lambda = Math.toRadians(longitude);
        double lambdaZone = Math.toRadians(zoneCentralMeridian);

        // Calculate intermediate terms
        double n = SEMI_MAJOR_AXIS / Math.sqrt(1 - ECCENTRICITY_SQUARED * Math.sin(phi) * Math.sin(phi));
        double t = Math.tan(phi) * Math.tan(phi);
        double c = ECCENTRICITY_SQUARED * Math.cos(phi) * Math.cos(phi) / (1 - ECCENTRICITY_SQUARED);

        double a = Math.cos(phi) * (lambda - lambdaZone);

        // Easting calculation
        double easting = k0 * n * (a +
                (1 - t + c) * Math.pow(a, 3) / 6 +
                (5 - 18 * t + t * t + 72 * c - 58 * ECCENTRICITY_SQUARED) * Math.pow(a, 5) / 120)
                + 500000.0;  // Add false easting

        // Northing calculation
        double m = SEMI_MAJOR_AXIS * ((1 - ECCENTRICITY_SQUARED / 4.0 -
                3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 64.0 -
                5 * Math.pow(ECCENTRICITY_SQUARED, 3) / 256.0) * phi -
                (3 * ECCENTRICITY_SQUARED / 8.0 +
                        3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 32.0 +
                        45 * Math.pow(ECCENTRICITY_SQUARED, 3) / 1024.0) * Math.sin(2 * phi) +
                (15 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 256.0 +
                        45 * Math.pow(ECCENTRICITY_SQUARED, 3) / 1024.0) * Math.sin(4 * phi) -
                (35 * Math.pow(ECCENTRICITY_SQUARED, 3) / 3072.0) * Math.sin(6 * phi));

        double northing = k0 * (m +
                n * Math.tan(phi) * (a * a / 2 +
                        (5 - t + 9 * c + 4 * c * c) * Math.pow(a, 4) / 24 +
                        (61 - 58 * t + t * t + 600 * c - 330 * ECCENTRICITY_SQUARED) * Math.pow(a, 6) / 720));

        return new double[]{easting, northing};
    }
}