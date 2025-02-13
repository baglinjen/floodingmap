package dk.itu.drawing.models;

import dk.itu.models.OsmElement;
import dk.itu.models.OsmWay;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapModelDb extends MapModel {
    public MapModelDb(double minLon,
                      double minLat,
                      double maxLat,
                      List<OsmWay> ways,
                      List<OsmElement> areaElements,
                      List<OsmElement> pathElements)
    {
        super();
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.elements.addAll(ways);
        this.pathElements.addAll(pathElements);
        this.areaElements.addAll(areaElements);
        sortAreaElements();
        splitAreaLayers();
        addPathElements();
    }

    private void sortAreaElements() {
        areaElements = areaElements.stream().sorted((o1, o2) -> {
            if (o1 instanceof OsmWay w1 && o2 instanceof OsmWay w2) {
                Rectangle2D r1 = w1.getShape().getBounds2D();
                Rectangle2D r2 = w2.getShape().getBounds2D();
                return (r1.getWidth()*r1.getHeight()) - (r2.getWidth()*r2.getHeight()) > 0 ? -1 : 1;
            } else {
                return 0;
            }
        }).toList();
    }

    private void splitAreaLayers() {
        int topMostLayer = 0;
        Map<Integer, MapModelDb.BoundedLayer> levels = new HashMap<>();
        levels.put(topMostLayer, new MapModelDb.BoundedLayer(null, new ArrayList<>()));

        for (OsmElement element : this.areaElements) {
            if (!(element instanceof OsmWay way)) {
                continue;
            }

            var topLevel = levels.get(topMostLayer);
            var shapeBounds = way.getShape().getBounds2D();
            if (topLevel.bounds == null) {
                // First element
                topLevel.bounds = shapeBounds;
                topLevel.elements.add(way);
            } else {
                var intersects = topLevel.bounds.intersects(shapeBounds);
                var contains = topLevel.bounds.contains(shapeBounds);
                if (intersects || contains) {
                    // Collides => create new layer
                    topMostLayer++;
                    List<OsmElement> newLayer = new ArrayList<>();
                    newLayer.add(way);
                    levels.put(topMostLayer, new MapModelDb.BoundedLayer(shapeBounds, newLayer));
                } else {
                    // Doesn't collide => increase bounds + add to layer
                    double minX = Math.min(shapeBounds.getMinX(), topLevel.bounds.getMinX());
                    double minY = Math.min(shapeBounds.getMinY(), topLevel.bounds.getMinY());
                    double maxX = Math.max(shapeBounds.getMaxX(), topLevel.bounds.getMaxX());
                    double maxY = Math.max(shapeBounds.getMaxY(), topLevel.bounds.getMaxY());
                    topLevel.bounds.setRect(minX, minY, maxX - minX, maxY - minY);
                    topLevel.elements.add(way);
                }
            }

        }

        for (MapModelDb.BoundedLayer layer : levels.values()) {
            layers.add(layer.elements);
        }
    }

    private void addPathElements() {
        layers.add(pathElements);
    }

    private static class BoundedLayer {
        public Rectangle2D bounds;
        public List<OsmElement> elements;
        public BoundedLayer(Rectangle2D bounds, List<OsmElement> elements) {
            this.bounds = bounds;
            this.elements = elements;
        }
    }
}
