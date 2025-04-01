package dk.itu.data.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.itu.common.utils.extensions.ArrayExtensions;
import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.*;
import java.util.List;
import java.util.stream.DoubleStream;

import static dk.itu.data.models.parser.ParserGeoJsonElement.contains;
import static dk.itu.util.ArrayUtils.appendExcludingN;
import static dk.itu.util.PolygonUtils.*;

public class GeoJsonParserResult {
    private List<ParserGeoJsonElement> geoJsonElements = new ArrayList<>();

    public void sanitize() {
        geoJsonElements = geoJsonElements.parallelStream().sorted(Comparator.comparing(ParserGeoJsonElement::getArea).reversed()).toList();

        for (int i = 0; i < geoJsonElements.size(); i++) {
            ParserGeoJsonElement e1 = geoJsonElements.get(i);
            for (int j = i + 1; j < geoJsonElements.size(); j++) {
                ParserGeoJsonElement e2 = geoJsonElements.get(j);
                if (e1.contains(e2)) {
                    // Check if inner is already added
                    var innerCovered = e1
                            .getInnerPolygons()
                            .parallelStream()
                            .anyMatch(innerPolygon -> contains(innerPolygon, e2));
                    if (!innerCovered) e1.addInnerPolygon(e2.getOuterPolygon());
                }
            }
        }

        geoJsonElements.parallelStream().forEach(ParserGeoJsonElement::calculateShape);
    }

    public void addWorldRoot(){
        double[] worldCurvePolygon = {
                180, -180, //NE
                -180, -180, //NW
                -180, 180, //SW
                180, 180  //SE
        };

        geoJsonElements = ArrayExtensions.appendSingle(geoJsonElements, new ParserGeoJsonElement(0, worldCurvePolygon));
    }

    public List<ParserGeoJsonElement> getGeoJsonElements() {
        return geoJsonElements;
    }

    public void addGeoJsonFile(GeoJsonFile geoJsonFile) {
        // Map height to coordinates
        Map<Float, List<GeoJsonFile.Feature.Geometry>> heightToGeometry = new HashMap<>();
        for (GeoJsonFile.Feature feature : geoJsonFile.features) {
            heightToGeometry.putIfAbsent(feature.properties.height, new ArrayList<>());
            heightToGeometry.get(feature.properties.height).add(feature.geometry);
        }

        List<GeoJsonHeightCurve> closedHeightCurves = new ArrayList<>();

        for (Float height : heightToGeometry.keySet()) {
            List<GeoJsonHeightCurve> heightCurvesForHeight = new ArrayList<>();
            List<GeoJsonHeightCurve> openHeightCurvesForHeight = new ArrayList<>();

            var geometries = heightToGeometry.get(height);

            for (GeoJsonFile.Feature.Geometry geometry : geometries) {
                var geometryCoords = geometry.coords;
                if (isClosed(geometryCoords)) {
                    // If closed => add to closed height curves => outers should be clockwise
                    heightCurvesForHeight.add(new GeoJsonHeightCurve(height, geometryCoords));
                } else {
                    // If open => add as open height curve
                    openHeightCurvesForHeight.add(new GeoJsonHeightCurve(height, geometryCoords));
                }
            }

            // Chain the opens height curves
            for (int i = 0; i < openHeightCurvesForHeight.size(); i++) {
                var ii = openHeightCurvesForHeight.get(i);

                if (isClosed(ii.outerPolygon)) {
                    // Test if i closes itself => ensure it is counterclockwise as outer
                    heightCurvesForHeight.add(ii);
                    openHeightCurvesForHeight.remove(ii);
                    i = -1;
                    continue;
                }

                for (int j = 0; j < openHeightCurvesForHeight.size(); j++) {
                    if (i == j || i == -1) continue;
                    var jj = openHeightCurvesForHeight.get(j);

                    switch (findOpenPolygonMatchType(ii.outerPolygon, jj.outerPolygon)) {
                        case FIRST_FIRST -> {
                            // Reverse geometryCoords and prepend openHeightCurve
                            ii.outerPolygon = appendExcludingN(reversePairs(jj.outerPolygon), ii.outerPolygon, 2);
                            openHeightCurvesForHeight.remove(j);
                            i = -1;
                        }
                        case FIRST_LAST -> {
                            // Append geometryCoords to openHeightCurve
                            ii.outerPolygon = appendExcludingN(jj.outerPolygon, ii.outerPolygon, 2);
                            openHeightCurvesForHeight.remove(j);
                            i = -1;
                        }
                        case LAST_FIRST -> {
                            // Prepend geometryCoords to openHeightCurve
                            ii.outerPolygon = appendExcludingN(ii.outerPolygon, jj.outerPolygon, 2);
                            openHeightCurvesForHeight.remove(j);
                            i = -1;
                        }
                        case LAST_LAST -> {
                            // Reverse geometryCoords and append openHeightCurve
                            ii.outerPolygon = appendExcludingN(ii.outerPolygon, reversePairs(jj.outerPolygon), 2);
                            openHeightCurvesForHeight.remove(j);
                            i = -1;
                        }
                    }
                }
            }

            closedHeightCurves.addAll(heightCurvesForHeight);
            closedHeightCurves.addAll(openHeightCurvesForHeight);
        }

        geoJsonElements = closedHeightCurves.parallelStream().map(GeoJsonHeightCurve::createParserGeoJsonElement).toList();
    }

    public static class GeoJsonHeightCurve {
        private final float height;
        private double[] outerPolygon;
        public GeoJsonHeightCurve(float height, double[] outerPolygon) {
            this.height = height;
            this.outerPolygon = outerPolygon;
        }

        public ParserGeoJsonElement createParserGeoJsonElement() {
            return new ParserGeoJsonElement(height, outerPolygon);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoJsonFile(@JsonProperty List<GeoJsonFile.Feature> features) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Feature(@JsonProperty GeoJsonFile.Feature.Property properties, @JsonProperty GeoJsonFile.Feature.Geometry geometry) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Property(@JsonProperty("hoejde") Float height) {}
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Geometry {
                public double[] coords;

                @JsonCreator
                public Geometry(@JsonProperty("coordinates") List<Double[]> coordinates) {
                    coords = coordinates
                            .parallelStream()
                            .flatMapToDouble(arr -> DoubleStream.of(arr[0], arr[1]))
                            .toArray();
                }
            }
        }
    }
}