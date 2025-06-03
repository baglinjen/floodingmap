package dk.itu.data.repositories;

import dk.itu.common.models.Drawable;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import it.unimi.dsi.fastutil.doubles.Double2ReferenceMap;

import java.util.List;

public interface OsmElementRepository {
    void add(List<OsmElement> osmElements);
    void addTraversable(List<OsmNode> nodes);
    void getOsmElementsScaled(double minLon, double minLat, double maxLon, double maxLat, double minBoundingBoxArea, Double2ReferenceMap<Drawable> osmElements);
    List<OsmNode> getTraversableOsmNodes();
    List<RTreeNode> getSpatialNodes();
    OsmNode getNearestTraversableOsmNode(double lon, double lat);
    void clearAll();
    double[] getBounds();
}