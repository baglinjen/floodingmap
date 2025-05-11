package dk.itu.data.models.db.osm;

import dk.itu.common.models.Colored;
import dk.itu.data.models.db.BoundingBox;

public abstract class OsmElement extends Colored {
    private final long id;
    private final BoundingBox boundingBox;
    private final double area;

    public OsmElement(long id, BoundingBox boundingBox, double area) {
        this.id = id;
        this.boundingBox = boundingBox;
        this.area = area;
    }

    public long getId() { return id; }
    public double getArea() { return area; }
    public BoundingBox getBoundingBox() { return boundingBox; }
}