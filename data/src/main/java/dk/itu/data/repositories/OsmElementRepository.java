package dk.itu.data.repositories;

import dk.itu.data.models.db.BoundingBox;
import dk.itu.data.models.db.osm.OsmElement;
import dk.itu.data.models.db.osm.OsmNode;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;

import java.util.List;

public interface OsmElementRepository {
    void add(List<ParserOsmElement> osmElements);
    void addTraversable(List<ParserOsmNode> nodes);
    List<OsmElement> getOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat);
    List<OsmNode> getTraversableOsmNodes();
    OsmNode getNearestTraversableOsmNode(double lon, double lat);
    void clearAll();
    BoundingBox getBounds();
}
