package dk.itu.data.services;

import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.parsers.GmlParser;
import dk.itu.data.repositories.HeightCurveRepository;
import dk.itu.data.repositories.HeightCurveRepositoryMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dk.itu.util.CoordinateUtils.wgsToUtm;

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

    public static List<double[]> splitBoundingBoxInUTMQuadrants(double minLon, double minLat, double maxLon, double maxLat) {
        var blUtm = wgsToUtm(minLon, minLat);
        var tlUtm = wgsToUtm(minLon, maxLat);
        var trUtm = wgsToUtm(maxLon, maxLat);
        var brUtm = wgsToUtm(maxLon, minLat);

        // Find floor utm rounded at 10km
        var minLonRounded = Math.floor(Math.min(blUtm[0], tlUtm[0]) / 10_000) * 10_000;
        var minLatRounded = Math.floor(Math.min(blUtm[1], brUtm[1]) / 10_000) * 10_000;
        // Find ceil utm rounded at 10km
        var maxLonRounded = Math.ceil(Math.max(brUtm[0], trUtm[0]) / 10_000) * 10_000;
        var maxLatRounded = Math.ceil(Math.max(tlUtm[1], trUtm[1]) / 10_000) * 10_000;

        List<double[]> quadrants = new ArrayList<>();
        quadrants.add(new double[] {minLonRounded, minLatRounded, maxLonRounded, maxLatRounded});

        while (!quadrants.parallelStream().allMatch(HeightCurveService::isQuadrant10Km)) {
            quadrants = quadrants.parallelStream().flatMap(HeightCurveService::split10KmQuadrants).toList();
        }

        return quadrants;
    }
    private static boolean isQuadrant10Km(double[] quadrant) {
        return
                quadrant[2] - quadrant[0] <= 10_000 &&
                quadrant[3] - quadrant[1] <= 10_000;
    }
    private static Stream<double[]> split10KmQuadrants(double[] quadrant) {
        var shouldSplitHorizontally = quadrant[2] - quadrant[0] > 10_000;
        var shouldSplitVertically = quadrant[3] - quadrant[1] > 10_000;

        if (shouldSplitHorizontally) {
            return Stream.of(
                    new double[] {
                            quadrant[0],
                            quadrant[1],
                            quadrant[0] + 10_000,
                            quadrant[3]
                    },
                    new double[] {
                            quadrant[0] + 10_000,
                            quadrant[1],
                            quadrant[2],
                            quadrant[3]
                    }
            );
        } else if (shouldSplitVertically) {
            return Stream.of(
                    new double[] {
                            quadrant[0],
                            quadrant[1],
                            quadrant[2],
                            quadrant[1] + 10_000,
                    },
                    new double[] {
                            quadrant[0],
                            quadrant[1] + 10_000,
                            quadrant[2],
                            quadrant[3]
                    }
            );
        } else {
            return Stream.of(quadrant);
        }
    }

    public void loadGmlData(double minLon, double minLat, double maxLon, double maxLat) {
        List<double[]> quadrants = splitBoundingBoxInUTMQuadrants(minLon, minLat, maxLon, maxLat);

        synchronized (this.repository) {
            HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult(this.repository);

            GmlParser.parse(quadrants, heightCurveParserResult);

            heightCurveParserResult.addUnconnectedElements(this.repository.getUnconnectedElements());

            heightCurveParserResult.sanitize();

            this.repository.add(heightCurveParserResult.getElements());
            this.repository.setUnconnectedElements(heightCurveParserResult.getUnconnectedElements());
        }
    }

    public void clearAll() {
        synchronized (this.repository) {
            this.repository.clear();
        }
    }
}