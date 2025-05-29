package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.data.repositories.HeightCurveRepository;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;

public class HeightCurveParserResult {
    private static final Logger logger = LoggerFactory.getLogger();
    private final Map<Float, List<ParserHeightCurveElement>> elementsByHeight = new HashMap<>();
    private final Map<Float, List<ParserHeightCurveElement>> unconnectedElements = Collections.synchronizedMap(new HashMap<>());
    private List<ParserHeightCurveElement> elements = Collections.synchronizedList(new ArrayList<>());

    private final HeightCurveRepository repository;

    public HeightCurveParserResult(HeightCurveRepository repository) {
        this.repository = repository;
    }

    public List<ParserHeightCurveElement> getElements() {
        return elements;
    }

    public Map<Float, List<ParserHeightCurveElement>> getUnconnectedElements() {
        return unconnectedElements;
    }

    public void sanitize() {
        logger.info("Starting to sanitize {} height curve elements", elementsByHeight.values().stream().mapToInt(List::size).sum());
        elementsByHeight.keySet().parallelStream().forEach(h -> this.closeHeightCurvesForH(h, elementsByHeight.get(h)));
        elementsByHeight.clear();
        logger.info("Split in {} elements and {} unconnected elements - sorting by area", elements.size(), unconnectedElements.size());
        elements.sort(Comparator.comparing(hc -> hc.getCoordinates().length));
        elements = elements.parallelStream().sorted(Comparator.comparing(hc -> hc.getCoordinates().length)).toList();
        logger.info("Finished sanitizing");
    }

    private void closeHeightCurvesForH(Float height, List<ParserHeightCurveElement> elements) {
        List<ParserHeightCurveElement> closedElements = new ArrayList<>();
        List<ParserHeightCurveElement> unclosedElements = new ArrayList<>(elements);

        for (int i = 0; i < unclosedElements.size(); i++) {
            // Iterate i trying to close j
            var polygonI = unclosedElements.get(i);
            // If i is closed already => add to closed and i-- && continue
            if (isClosed(polygonI.getCoordinates())) {
                unclosedElements.remove(polygonI);
                closedElements.add(polygonI);
                i--;
                continue;
            }

            for (int j = i + 1; j < unclosedElements.size(); j++) {
                var polygonJ = unclosedElements.get(j);

                // If i is added to j => skip through j continue with i (j = unclosedElements.size & i--)
                switch (findOpenPolygonMatchType(polygonI.getCoordinates(), polygonJ.getCoordinates())) {
                    case FIRST_FIRST -> {
                        // Remove i to add to j
                        unclosedElements.remove(polygonI);

                        // Add i to j
                        reversePairsMut(polygonI.getCoordinates());
                        polygonJ.setCoordinates(appendExcludingN(polygonI.getCoordinates(), polygonJ.getCoordinates(), 2));
                        polygonJ.addGmlIds(polygonI.getGmlIds());

                        // Add j to closed if it is now closed
                        if (isClosed(polygonJ.getCoordinates())) {
                            unclosedElements.remove(polygonJ);
                            closedElements.add(polygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = unclosedElements.size();
                        i--;
                    }
                    case LAST_FIRST -> {
                        // Remove i to add to j
                        unclosedElements.remove(polygonI);

                        // Add i to j
                        polygonJ.setCoordinates(appendExcludingN(polygonI.getCoordinates(), polygonJ.getCoordinates(), 2));
                        polygonJ.addGmlIds(polygonI.getGmlIds());

                        // Add j to closed if it is now closed
                        if (isClosed(polygonJ.getCoordinates())) {
                            unclosedElements.remove(polygonJ);
                            closedElements.add(polygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = unclosedElements.size();
                        i--;
                    }
                    case FIRST_LAST -> {
                        // Remove i to add to j
                        unclosedElements.remove(polygonI);

                        // Add i to j
                        polygonJ.setCoordinates(appendExcludingN(polygonJ.getCoordinates(), polygonI.getCoordinates(), 2));
                        polygonJ.addGmlIds(polygonI.getGmlIds());

                        // Add j to closed if it is now closed
                        if (isClosed(polygonJ.getCoordinates())) {
                            unclosedElements.remove(polygonJ);
                            closedElements.add(polygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = unclosedElements.size();
                        i--;
                    }
                    case LAST_LAST -> {
                        // Remove i to add to j
                        unclosedElements.remove(polygonI);

                        // Add i to j
                        reversePairsMut(polygonI.getCoordinates());
                        polygonJ.setCoordinates(appendExcludingN(polygonJ.getCoordinates(), polygonI.getCoordinates(), 2));
                        polygonJ.addGmlIds(polygonI.getGmlIds());

                        // Add j to closed if it is now closed
                        if (isClosed(polygonJ.getCoordinates())) {
                            unclosedElements.remove(polygonJ);
                            closedElements.add(polygonJ);
                        }

                        // Reset loop to restart with new element i
                        j = unclosedElements.size();
                        i--;
                    }
                }
            }
        }

        synchronized (this.elements) {
            this.elements.addAll(closedElements);
        }
        if (!unclosedElements.isEmpty()) {
            synchronized (this.unconnectedElements) {
                this.unconnectedElements.computeIfAbsent(height, _ -> new ArrayList<>()).addAll(unclosedElements);
            }
        }
    }

    public void addUnconnectedElements(Map<Float, List<ParserHeightCurveElement>> unconnectedElements) {
        this.elementsByHeight.putAll(unconnectedElements);
    }

    public synchronized void addParsedElementSync(ParserHeightCurveElement element) {
        if (!this.repository.hasIdsBeenParsed(element.getGmlIds().getFirst())) {
            this.repository.addParsedId(element.getGmlIds().getFirst());
            var height = element.getHeight();
            elementsByHeight.putIfAbsent(height, new ArrayList<>());
            elementsByHeight.get(height).add(element);
        }
    }
}