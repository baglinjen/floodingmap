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
}