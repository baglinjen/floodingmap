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
    private List<ParserHeightCurveElement> elements = new ArrayList<>();
    private final List<ParserHeightCurveElement> unconnectedElements = new ArrayList<>();

    private final HeightCurveRepository repository;

    public HeightCurveParserResult(HeightCurveRepository repository) {
        this.repository = repository;
    }

    public List<ParserHeightCurveElement> getElements() {
        return elements;
    }

    public List<ParserHeightCurveElement> getUnconnectedElements() {
        return unconnectedElements;
    }

    public void sanitize() {
        logger.info("Starting to sanitize {} height curve elements", elementsByHeight.values().stream().mapToInt(List::size).sum());
        elementsByHeight.values().forEach(list -> closeHeightCurvesForHeight(list, elements, unconnectedElements));
        elementsByHeight.clear();
        logger.info("Split in {} elements and {} unconnected elements - sorting by area", elements.size(), unconnectedElements.size());
        elements = elements.parallelStream().sorted(Comparator.comparing(ParserHeightCurveElement::calculateArea)).toList();
        logger.info("Finished sanitizing");
    }

    private void closeHeightCurvesForHeight(List<ParserHeightCurveElement> elements, List<ParserHeightCurveElement> closedElements, List<ParserHeightCurveElement> unclosedElements) {
        for (int i = 0; i < elements.size(); i++) {
            var elementI = elements.get(i);
            var coordsI = elementI.getCoordinates();

            if (isClosed(coordsI)) {
                continue;
            }

            for (int j = i; j < elements.size(); j++) {
                if (i == j || i == -1) continue;
                var elementJ = elements.get(j);
                var coordsJ = elementJ.getCoordinates();

                switch (findOpenPolygonMatchType(coordsI, coordsJ)) {
                    case FIRST_FIRST -> {
                        elementJ.setCoordinates(appendExcludingN(reversePairs(coordsJ), coordsI, 2));
                        elementJ.addGmlIds(elementI.getGmlIds());
                        elements.remove(i);
                        i = -1;
                    }
                    case FIRST_LAST -> {
                        elementJ.setCoordinates(appendExcludingN(coordsJ, coordsI, 2));
                        elementJ.addGmlIds(elementI.getGmlIds());
                        elements.remove(i);
                        i = -1;
                    }
                    case LAST_FIRST -> {
                        elementJ.setCoordinates(appendExcludingN(coordsI, coordsJ, 2));
                        elementJ.addGmlIds(elementI.getGmlIds());
                        elements.remove(i);
                        i = -1;
                    }
                    case LAST_LAST -> {
                        elementJ.setCoordinates(appendExcludingN(coordsI, reversePairs(coordsJ), 2));
                        elementJ.addGmlIds(elementI.getGmlIds());
                        elements.remove(i);
                        i = -1;
                    }
                }
            }
        }

        for (ParserHeightCurveElement element : elements) {
            if (isClosed(element.getCoordinates())) {
                closedElements.add(element);
            } else {
                unclosedElements.add(element);
            }
        }
    }

    public void addUnconnectedElements(List<ParserHeightCurveElement> elements) {
        for (ParserHeightCurveElement element : elements) {
            addElement(element);
        }
    }

    public synchronized void addParsedElementSync(ParserHeightCurveElement element) {
        if (!this.repository.hasIdsBeenParsed(element.getGmlIds().getFirst())) {
            this.repository.addParsedId(element.getGmlIds().getFirst());
            var height = element.getHeight();
            elementsByHeight.putIfAbsent(height, new ArrayList<>());
            elementsByHeight.get(height).add(element);
        }
    }

    private void addElement(ParserHeightCurveElement element) {
        var height = element.getHeight();
        elementsByHeight.putIfAbsent(height, new ArrayList<>());
        elementsByHeight.get(height).add(element);
    }
}