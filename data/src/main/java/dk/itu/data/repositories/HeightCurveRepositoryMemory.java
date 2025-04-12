package dk.itu.data.repositories;

import dk.itu.data.datastructure.heightcurvetree.HeightCurveTree;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeightCurveRepositoryMemory implements HeightCurveRepository {
    private final Logger logger = LoggerFactory.getLogger();
    private static HeightCurveRepositoryMemory instance;

    public static HeightCurveRepositoryMemory getInstance() {
        if (instance == null) {
            instance = new HeightCurveRepositoryMemory();
        }
        return instance;
    }

    private final Set<Long> parsedIds = new HashSet<>();
    private final HeightCurveTree heightCurveTree = new HeightCurveTree();
    private final List<ParserHeightCurveElement> unconnectedElements = new ArrayList<>();

    private HeightCurveRepositoryMemory() {}

    @Override
    public void add(List<ParserHeightCurveElement> elements) {
        System.out.println();
        for (ParserHeightCurveElement element : elements) {
            var elementGmlIds = element.getGmlIds();
            if (parsedIds.containsAll(elementGmlIds)) {
                logger.warn("Duplicate height curve element: {}", elementGmlIds);
            } else {
                parsedIds.addAll(elementGmlIds);
                heightCurveTree.put(element);
            }
        }
        System.out.println();
    }

    @Override
    public List<ParserHeightCurveElement> getUnconnectedElements() {
        return unconnectedElements;
    }

    @Override
    public void setUnconnectedElements(List<ParserHeightCurveElement> elements) {
        System.out.println();
        unconnectedElements.clear();
        unconnectedElements.addAll(elements);
        System.out.println();
    }

    @Override
    public List<HeightCurveElement> getElements() {
        return heightCurveTree.getElements();
    }

    @Override
    public List<List<HeightCurveElement>> getFloodingSteps(float waterLevel) {
        return heightCurveTree.getFloodingStepsConcurrent(waterLevel);
    }

    @Override
    public HeightCurveElement getHeightCurveForPoint(double lon, double lat) {
        return heightCurveTree.getHeightCurveForPoint(lon, lat);
    }

    @Override
    public float getMinWaterLevel() {
        return heightCurveTree.getMinWaterLevel();
    }

    @Override
    public float getMaxWaterLevel() {
        return heightCurveTree.getMaxWaterLevel();
    }

    @Override
    public void clear() {
        unconnectedElements.clear();
        heightCurveTree.clear();
    }
}
