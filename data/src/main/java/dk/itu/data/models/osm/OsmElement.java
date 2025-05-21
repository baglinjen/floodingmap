package dk.itu.data.models.osm;

import dk.itu.common.models.WithId;
import dk.itu.data.models.BoundingBox;

public abstract class OsmElement extends BoundingBox implements WithId {
    private final long id;

    public OsmElement(long id, double[] boundingBox) {
        super(boundingBox);
        this.id = id;
    }

    public long getId() { return id; }
}