package dk.itu.data.repositories;

import dk.itu.data.datastructure.heightcurvetree.HeightCurveTree;
import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.fury.shaded.org.codehaus.commons.compiler.java8.java.util.stream.Stream;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

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
    public synchronized Set<Long> getParsedIds() {
        return parsedIds;
    }

    @Override
    public synchronized void add(List<ParserHeightCurveElement> elements) {
        for (ParserHeightCurveElement element : elements) {
//            var elementGmlIds = element.getGmlIds();
//            if (parsedIds.containsAll(elementGmlIds)) {
//                logger.warn("Duplicate height curve element: {}", elementGmlIds);
//            } else {
//                parsedIds.addAll(elementGmlIds);
//                heightCurveTree.put(element);
//            }
            if (element.getGmlIds().parallelStream().anyMatch(parsedIds::contains)) {
                logger.warn("Duplicate id: {}", element.getGmlIds());
            }
            parsedIds.addAll(element.getGmlIds());
            heightCurveTree.put(element);
        }
    }

    @Override
    public synchronized List<ParserHeightCurveElement> getUnconnectedElements() {
        return unconnectedElements;
    }

    @Override
    public synchronized void setUnconnectedElements(List<ParserHeightCurveElement> elements) {
        unconnectedElements.clear();
        unconnectedElements.addAll(elements);
        parsedIds.addAll(elements.parallelStream().map(ParserHeightCurveElement::getGmlIds).flatMap(Collection::stream).toList());
    }

    @Override
    public synchronized List<HeightCurveElement> getElements() {
        synchronized (heightCurveTree) {
            return heightCurveTree.getElements();
        }
    }

    @Override
    public synchronized List<List<HeightCurveElement>> getFloodingSteps(float waterLevel) {
        return heightCurveTree.getFloodingStepsConcurrent(waterLevel);
    }

    @Override
    public synchronized HeightCurveElement getHeightCurveForPoint(double lon, double lat) {
        return heightCurveTree.getHeightCurveForPoint(lon, lat);
    }

    @Override
    public synchronized float getMinWaterLevel() {
        return heightCurveTree.getMinWaterLevel();
    }

    @Override
    public synchronized float getMaxWaterLevel() {
        return heightCurveTree.getMaxWaterLevel();
    }

    @Override
    public synchronized void clear() {
        unconnectedElements.clear();
        heightCurveTree.clear();
    }
}
