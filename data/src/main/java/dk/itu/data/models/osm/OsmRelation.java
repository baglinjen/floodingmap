package dk.itu.data.models.osm;

import java.util.List;

public class OsmRelation {
    private long id;
    private List<OsmWay> ways;  // Relations primarily contain ways, but can also have nodes or other relations

    public OsmRelation(long id, List<OsmWay> ways) {
        this.id = id;
        this.ways = ways;
    }

    public long getId() {
        return id;
    }

    public List<OsmWay> getWays() {
        return ways;
    }
}
