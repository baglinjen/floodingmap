package dk.itu.drawing.models;

import dk.itu.models.OsmElement;
import dk.itu.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static dk.itu.utils.GeneralUtils.splitList;

public class MapModelOsmFile extends MapModel {
    public MapModelOsmFile(double minLon, double minLat, double maxLat, double maxLon, List<OsmElement> areaElements, List<OsmElement> pathElements) {
        super();
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.maxLon = maxLon;

        AtomicReference<List<OsmElement>> atomicSortedAreaElements = new AtomicReference<>(new ArrayList<>());

        TimeUtils.timeFunction("Splitting layers", () -> {
            atomicSortedAreaElements.updateAndGet(list -> {
                list = areaElements.stream().parallel().sorted(Comparator.comparing(OsmElement::getArea).reversed()).collect(Collectors.toList());
                list.addAll(pathElements);
                return list;
            });
        });

        this.layers = splitList(atomicSortedAreaElements.get(), AREA_LAYERS);
    }
}