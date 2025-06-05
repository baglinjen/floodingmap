package dk.itu.data.repositories;

import dk.itu.data.datastructure.heightcurvetree.HeightCurveTree;
import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class HeightCurveRepositoryMemory implements HeightCurveRepository {
    private static final Logger logger = LoggerFactory.getLogger();
    private static HeightCurveRepositoryMemory instance;

    public static HeightCurveRepositoryMemory getInstance() {
        if (instance == null) {
            instance = new HeightCurveRepositoryMemory();
        }
        return instance;
    }

    private final Set<Long> parsedIds = new ConcurrentSkipListSet<>();
    private final HeightCurveTree heightCurveTree = new HeightCurveTree();
    private final Map<Float, List<ParserHeightCurveElement>> unconnectedElementsMap = new HashMap<>();

    private HeightCurveRepositoryMemory() {}

    @Override
    public void addParsedId(long id) {
        parsedIds.add(id);
    }

    @Override
    public boolean hasIdsBeenParsed(long id) {
        return parsedIds.contains(id);
    }

    @Override
    public void add(List<ParserHeightCurveElement> elements) {
        logger.info("Adding {} height curve elements", elements.size());
        long startTime = System.nanoTime();

        int elementsAdded = 0;

        for (ParserHeightCurveElement element : elements) {
            if (!element.getGmlIds().parallelStream().allMatch(parsedIds::contains)) {
                logger.error("Found height curve element where not all gml ids have yet been added : {}", element.getGmlIds());
            }

            heightCurveTree.put(HeightCurveElement.mapToHeightCurveElement(element));
            elementsAdded++;
            if (elementsAdded % 5_000 == 0) logger.debug("Added {}/{} height curve elements", elementsAdded, elements.size());
        }

        logger.info("Finished adding {} height curve elements in {}ms", elements.size(), String.format("%.3f", (System.nanoTime() - startTime) / 1_000_000d));
    }

    @Override
    public Map<Float, List<ParserHeightCurveElement>> getUnconnectedElements() {
        return unconnectedElementsMap;
    }

    @Override
    public void setUnconnectedElements(Map<Float, List<ParserHeightCurveElement>> elements) {
        logger.info("Updating unconnected elements for {} elements", elements.size());
        unconnectedElementsMap.clear();
        unconnectedElementsMap.putAll(elements);
        parsedIds.addAll(elements.values().parallelStream().flatMap(List::stream).map(ParserHeightCurveElement::getGmlIds).flatMap(Collection::stream).toList());
        logger.info("Finished updating unconnected elements for {} elements", elements.size());
    }

    @Override
    public void getElements(List<HeightCurveElement> heightCurves) {
        this.heightCurveTree.getElements(heightCurves);
    }

    @Override
    public List<List<HeightCurveElement>> getFloodingSteps(float waterLevel) {
        return heightCurveTree.getFloodingSteps(waterLevel);
    }

    @Override
    public HeightCurveElement getHeightCurveForPoint(float lon, float lat) {
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
    public synchronized void clear() {
        parsedIds.clear();
        unconnectedElementsMap.clear();
        heightCurveTree.clear();
    }
}
