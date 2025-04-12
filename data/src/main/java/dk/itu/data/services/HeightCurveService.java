package dk.itu.data.services;

import dk.itu.data.dto.HeightCurveParserResult;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.parsers.GmlParser;
import dk.itu.data.repositories.HeightCurveRepository;
import dk.itu.data.repositories.HeightCurveRepositoryMemory;

import java.util.List;

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
            HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult();

            // Get data from OSM file
            GmlParser.parse(gmlFile, heightCurveParserResult);

            // Add previously unconnected elements
            heightCurveParserResult.addElements(repository.getUnconnectedElements());

            heightCurveParserResult.sanitize();

            repository.add(heightCurveParserResult.getElements());
            repository.setUnconnectedElements(heightCurveParserResult.getUnconnectedElements());
        }
    }

    public void loadGmlData(double minLon, double minLat, double maxLon, double maxLat) {
        synchronized (this.repository) {
            HeightCurveParserResult heightCurveParserResult = new HeightCurveParserResult();

            // Get data from OSM file
            GmlParser.parse(minLon, minLat, maxLon, maxLat, heightCurveParserResult);

            // Add previously unconnected elements
            heightCurveParserResult.addElements(repository.getUnconnectedElements());

            heightCurveParserResult.sanitize();

            repository.add(heightCurveParserResult.getElements());
            repository.setUnconnectedElements(heightCurveParserResult.getUnconnectedElements());
        }
    }
}
