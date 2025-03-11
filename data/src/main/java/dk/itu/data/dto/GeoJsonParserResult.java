package dk.itu.data.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.itu.common.models.geojson.GeoJsonElement;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;

public class GeoJsonParserResult {
    private List<GeoJsonElement> geoJsonElements = new ArrayList<>();
    private final GeoJsonElement root = new GeoJsonElement(0, new Path2D.Double());
    private final Map<GeoJsonElement, List<GeoJsonElement>> connections = new HashMap<>();

    public void sanitize() {
        geoJsonElements = geoJsonElements.parallelStream().sorted(Comparator.comparing(GeoJsonElement::getHeight)).toList();

        var elementsByArea = geoJsonElements.parallelStream().sorted(Comparator.comparing(GeoJsonElement::getAbsoluteArea).reversed()).toList();

        connections.put(root, new ArrayList<>());
        for (int i = 0; i < elementsByArea.size(); i++) {
            GeoJsonElement element = elementsByArea.get(i);
            var pathElement = (Path2D.Double) element.getShape();
            insertsNthElement(elementsByArea, element, pathElement, i);
        }
        System.out.println();
    }

    public GeoJsonElement getRoot() {
        return root;
    }

    public Map<GeoJsonElement, List<GeoJsonElement>> getConnections() {
        return connections;
    }

    private void insertsNthElement(List<GeoJsonElement> elementsByArea, GeoJsonElement element, Path2D.Double pathElement, int i) {
        if (i == 0) {
            connections.get(root).add(element);
            connections.putIfAbsent(element, new ArrayList<>());
            return;
        }

        // Nth element
        GeoJsonElement elementBefore = elementsByArea.get(i - 1);
        var pathBefore = (Path2D.Double) elementBefore.getShape();

        if (fullyContains(pathBefore, pathElement)) {
            // Path before contains element i
            connections.get(elementBefore).add(element);
            connections.putIfAbsent(element, new ArrayList<>());
        } else {
            // Path doesn't contain
            insertsNthElement(elementsByArea, element, pathElement, i - 1);
        }
    }

    public boolean fullyContains(Path2D container, Path2D contained) {
        // Create Area objects from the paths
        Area containerArea = new Area(container);

        // Create a copy of the contained area
        Area copyContainedArea = new Area(contained);

        // Subtract the container from the contained copy
        copyContainedArea.subtract(containerArea);

        // If the result is empty, the contained path is fully within the container
        return copyContainedArea.isEmpty();
    }

    public List<GeoJsonElement> getGeoJsonElements() {
        return geoJsonElements;
    }

    public void addGeoJsonFile(GeoJsonFile geoJsonFile) {
        Map<Float, List<GeoJsonFile.Feature.Geometry>> heightToCoordinates = new HashMap<>();
        for (GeoJsonFile.Feature feature : geoJsonFile.features) {
            heightToCoordinates.putIfAbsent(feature.properties.height, new ArrayList<>());
            heightToCoordinates.get(feature.properties.height).add(feature.geometry);
        }

        for (Float height : heightToCoordinates.keySet()) {
            List<Path2D> closedPaths = new ArrayList<>();
            List<GeoJsonFile.Feature.Geometry> openGeometries = new ArrayList<>();
            var geometries = heightToCoordinates.get(height);

            for (GeoJsonFile.Feature.Geometry geometry : geometries) {
                if (geometryClosesItself(geometry)) {
                    Path2D path = new Path2D.Double();
                    path.moveTo(0.56*geometry.coordinates.getFirst().longitude, -geometry.coordinates.getFirst().latitude);
                    for (int i = 1; i < geometry.coordinates.size() - 1; i++) {
                        path.lineTo(0.56*geometry.coordinates.get(i).longitude, -geometry.coordinates.get(i).latitude);
                    }
                    path.closePath();
                    closedPaths.add(path);
                } else {
                    openGeometries.add(geometry);
                }
            }

            for (int i = 0; i < openGeometries.size(); i++) {
                var currentGeometry = openGeometries.get(i);
                var currentGeometryFirst = currentGeometry.coordinates.getFirst();
                var currentGeometryLast = currentGeometry.coordinates.getLast();

                for (int j = i + 1; j < openGeometries.size(); j++) {
                    var geometryJ = openGeometries.get(j);
                    var geometryJFirst = geometryJ.coordinates.getFirst();
                    var geometryJLast = geometryJ.coordinates.getLast();

                    if (currentGeometryFirst.latitude == geometryJFirst.latitude && currentGeometryFirst.longitude == geometryJFirst.longitude) {
                        // Same first

                        var newList = new ArrayList<>(geometryJ.coordinates.reversed().stream().toList().subList(0, geometryJ.coordinates.size() - 1));
                        newList.addAll(currentGeometry.coordinates);
                        currentGeometry.replaceCoordinates(newList);

                        openGeometries.remove(j);
                        i = -1;
                        break;
                    } else if (currentGeometryLast.latitude == geometryJLast.latitude && currentGeometryLast.longitude == geometryJLast.longitude) {
                        // Same last

                        var newList = new ArrayList<>(geometryJ.coordinates.reversed().stream().toList().subList(1, geometryJ.coordinates.size()));
                        newList.addAll(currentGeometry.coordinates);
                        currentGeometry.replaceCoordinates(newList);

                        openGeometries.remove(j);
                        i = -1;
                        break;
                    } else if (currentGeometryFirst.latitude == geometryJLast.latitude && currentGeometryFirst.longitude == geometryJLast.longitude) {
                        // Same i first as j last
                        var newList = new ArrayList<>(geometryJ.coordinates.subList(0, geometryJ.coordinates.size() - 1));
                        newList.addAll(currentGeometry.coordinates);
                        geometryJ.replaceCoordinates(newList);

                        openGeometries.remove(i);
                        i = -1;
                        break;
                    } else if (currentGeometryLast.latitude == geometryJFirst.latitude && currentGeometryLast.longitude == geometryJFirst.longitude) {
                        // Same i last as j first
                        var newList = new ArrayList<>(currentGeometry.coordinates.subList(0, currentGeometry.coordinates.size() - 1));
                        newList.addAll(geometryJ.coordinates);
                        currentGeometry.replaceCoordinates(newList);

                        openGeometries.remove(j);
                        i = -1;
                        break;
                    }
                }
            }

            // Geometries should now be closing themselves
            for (GeoJsonFile.Feature.Geometry openGeometry : openGeometries) {
                Path2D path = new Path2D.Double();
                path.moveTo(0.56*openGeometry.coordinates.getFirst().longitude, -openGeometry.coordinates.getFirst().latitude);
                for (int i = 1; i < openGeometry.coordinates.size() - 1; i++) {
                    path.lineTo(0.56*openGeometry.coordinates.get(i).longitude, -openGeometry.coordinates.get(i).latitude);
                }
                path.closePath();
                closedPaths.add(path);
            }

            for (Path2D closedPath : closedPaths) {
                geoJsonElements.add(new GeoJsonElement(height, closedPath));
            }
        }
    }

    private boolean geometryClosesItself(GeoJsonFile.Feature.Geometry geometry) {
        var coordFirst = geometry.coordinates.getFirst();
        var coordLast = geometry.coordinates.getLast();

        return coordFirst.latitude == coordLast.latitude && coordFirst.longitude == coordLast.longitude;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeoJsonFile(@JsonProperty List<GeoJsonFile.Feature> features) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Feature(@JsonProperty GeoJsonFile.Feature.Property properties, @JsonProperty GeoJsonFile.Feature.Geometry geometry) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Property(@JsonProperty("hoejde") Float height) {}
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Geometry {
                public List<GeoJsonFile.Feature.Geometry.Coordinate> coordinates = new ArrayList<>();

                @JsonCreator
                public Geometry(@JsonProperty("coordinates") List<Double[]> rawCoordinates) {
                    for (Double[] coordinate : rawCoordinates) {
                        coordinates.add(new GeoJsonFile.Feature.Geometry.Coordinate(coordinate[0], coordinate[1]));
                    }
                }

                public void replaceCoordinates(List<GeoJsonFile.Feature.Geometry.Coordinate> coordinates) {
                    this.coordinates = coordinates;
                }

                public List<GeoJsonFile.Feature.Geometry.Coordinate> getCoordinates() {
                    return coordinates;
                }

                public record Coordinate(double longitude, double latitude) {}
            }
        }
    }
}