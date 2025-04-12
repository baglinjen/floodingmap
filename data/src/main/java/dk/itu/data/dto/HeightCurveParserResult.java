package dk.itu.data.dto;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;

import java.util.*;

import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;

public class HeightCurveParserResult {
    private final Map<Float, List<ParserHeightCurveElement>> elementsByHeight = new HashMap<>();
    private final List<ParserHeightCurveElement> elements = new ArrayList<>();
    private final List<ParserHeightCurveElement> unconnectedElements = new ArrayList<>();

    public List<ParserHeightCurveElement> getElements() {
        return elements;
    }

    public List<ParserHeightCurveElement> getUnconnectedElements() {
        return unconnectedElements;
    }

    public void sanitize() {
        elementsByHeight.values().parallelStream().forEach(this::closeHeightCurvesForHeight);
        elements.addAll(
                elementsByHeight
                        .values()
                        .parallelStream()
                        .flatMap(Collection::stream)
                        .toList()
        );
        elementsByHeight.clear();
    }

    private void closeHeightCurvesForHeight(List<ParserHeightCurveElement> elements) {
        for (int i = 0; i < elements.size(); i++) {
            var elementI = elements.get(i);
            var coordsI = elementI.getCoordinates();

            if (isClosedWithTolerance(coordsI)) {
                continue;
            }

            for (int j = i; j < elements.size(); j++) {
                if (i == j || i == -1) continue;
                var elementJ = elements.get(j);
                var coordsJ = elementJ.getCoordinates();

                switch (findOpenPolygonMatchTypeWithTolerance(coordsI, coordsJ)) {
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

        unconnectedElements.addAll(
                elements
                        .parallelStream()
                        .filter(e -> !isClosedWithTolerance(e.getCoordinates()))
                        .toList()
        );
    }

    public void addElements(List<ParserHeightCurveElement> elements) {
        for (ParserHeightCurveElement element : elements) {
            addElement(element);
        }
    }

    public void addElement(ParserHeightCurveElement element) {
        var height = element.getHeight();
        elementsByHeight.putIfAbsent(height, new ArrayList<>());
        elementsByHeight.get(height).add(element);
    }
}
