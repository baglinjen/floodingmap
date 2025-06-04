package dk.itu.data.repositories;

import dk.itu.common.models.Drawable;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;

import java.util.List;

public interface OsmElementRepository {
    void add(List<OsmElement> osmElements);
    void addTraversable(List<OsmNode> nodes);
    void getOsmElementsScaled(float minLon, float minLat, float maxLon, float maxLat, float minBoundingBoxArea, Float2ReferenceMap<Drawable> osmElements);
    List<OsmNode> getTraversableOsmNodes();
    List<RTreeNode> getSpatialNodes();
    OsmNode getNearestTraversableOsmNode(float lon, float lat);
    void clearAll();
    float[] getBounds();
}