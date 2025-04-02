package dk.itu.data.repositories;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.db.Bounds;
import dk.itu.data.models.parser.ParserOsmElement;

import java.util.List;

public interface OsmElementRepository {
    void add(List<ParserOsmElement> osmElements);
    List<OsmElement> getOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat);
    Bounds getBounds();
}
