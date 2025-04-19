package dk.itu.data.services;

import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.parsers.GmlParser;
import dk.itu.data.repositories.HeightCurveRepository;
import dk.itu.data.repositories.HeightCurveRepositoryMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dk.itu.util.CoordinateUtils.haversineDistance;

public class HeightCurveService {
    private final HeightCurveRepository repository;

    public HeightCurveService() {
        this.repository = HeightCurveRepositoryMemory.getInstance();
    }

    public List<HeightCurveElement> getElements() {
        synchronized (this.repository) {
            return this.repository.getElements();
        }
    }

    public List<List<HeightCurveElement>> getFloodingSteps(float waterLevel) {
        synchronized (this.repository) {
            return this.repository.getFloodingSteps(waterLevel);
        }
    }

    public HeightCurveElement getHeightCurveForPoint(double lon, double lat) {
        synchronized (this.repository) {
            return this.repository.getHeightCurveForPoint(lon, lat);
        }
    }

    public float getMinWaterLevel() {
        synchronized (this.repository) {
            return this.repository.getMinWaterLevel();
        }
    }
    public float getMaxWaterLevel() {
        synchronized (this.repository) {
            return this.repository.getMaxWaterLevel();
        }
    }

    public void loadGmlFileData(String gmlFile) {
        synchronized (this.repository) {
            HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult(this.repository);

            // Get data from OSM file
            GmlParser.parse(gmlFile, heightCurveParserResult);

            // Add previously unconnected elements
            heightCurveParserResult.addUnconnectedElements(repository.getUnconnectedElements());

            heightCurveParserResult.sanitize();

            repository.add(heightCurveParserResult.getElements());
            repository.setUnconnectedElements(heightCurveParserResult.getUnconnectedElements());
        }
    }

    private static final double MAX_AREA_PRE_QUADRANT = 10, OVERLAP = 0.0001;
    private static boolean isQuadrantSmallEnough(double[] quadrant) {
        double width = haversineDistance(quadrant[0], quadrant[1], quadrant[2], quadrant[1]);
        double height = haversineDistance(quadrant[0], quadrant[1], quadrant[0], quadrant[3]);
        double areaKm2 = width * height;
        return areaKm2 <= MAX_AREA_PRE_QUADRANT;
    }
    private static Stream<double[]> splitQuadrant(double[] quadrant) {
        double minLon = quadrant[0];
        double minLat = quadrant[1];
        double maxLon = quadrant[2];
        double maxLat = quadrant[3];
        double width = haversineDistance(minLon, minLat, maxLon, minLat);
        double height = haversineDistance(minLon, minLat, minLon, maxLat);
        double areaKm2 = width * height;

        if (areaKm2 >= MAX_AREA_PRE_QUADRANT) {
            // Split
            if (width > height) {
                // Split on width
                return Stream.of(
                        new double[]{minLon - OVERLAP, minLat - OVERLAP, OVERLAP + (minLon + (maxLon - minLon) / 2), OVERLAP + maxLat},
                        new double[]{(minLon + (maxLon - minLon) / 2) - OVERLAP, minLat - OVERLAP, OVERLAP + maxLon, OVERLAP + maxLat}
                );
            } else {
                // Split on height
                return Stream.of(
                        new double[]{minLon - OVERLAP, minLat - OVERLAP, OVERLAP + maxLon, OVERLAP + (minLat + (maxLat - minLat) / 2)},
                        new double[]{minLon - OVERLAP, (minLat + (maxLat - minLat) / 2) - OVERLAP, OVERLAP + maxLon, OVERLAP + maxLat}
                );
            }
        } else {
            return Stream.of(quadrant);
        }
    }

    public void loadGmlData(double minLon, double minLat, double maxLon, double maxLat) {
        List<double[]> quadrants = new ArrayList<>();
        quadrants.add(new double[]{minLon - OVERLAP, minLat - OVERLAP, OVERLAP + maxLon, OVERLAP + maxLat});

        while (!quadrants.parallelStream().allMatch(HeightCurveService::isQuadrantSmallEnough)) {
            quadrants = quadrants.parallelStream().flatMap(HeightCurveService::splitQuadrant).toList();
        }

        synchronized (this.repository) {
            HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult(this.repository);

            GmlParser.parse(quadrants, heightCurveParserResult);

            heightCurveParserResult.addUnconnectedElements(this.repository.getUnconnectedElements());

            heightCurveParserResult.sanitize();

            this.repository.add(heightCurveParserResult.getElements());
            this.repository.setUnconnectedElements(heightCurveParserResult.getUnconnectedElements());
        }
    }
}
