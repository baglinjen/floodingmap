package dk.itu.data.repositories;

import dk.itu.data.models.BoundingBox;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;

import java.util.List;

public interface OsmElementRepository {
    void add(List<OsmElement> osmElements);
    void addTraversable(List<OsmNode> nodes);
    void getOsmElementsScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea, Float2ReferenceMap<OsmElement> osmElements);
    List<OsmNode> getTraversableOsmNodes();
    List<BoundingBox> getSpatialNodes();
    OsmNode getNearestTraversableOsmNode(double lon, double lat);
    void clearAll();
    double[] getBounds();
}