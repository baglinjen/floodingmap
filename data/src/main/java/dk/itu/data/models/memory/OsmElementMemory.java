package dk.itu.data.models.memory;

import dk.itu.common.models.Colored;
import dk.itu.common.models.OsmElement;

public abstract class OsmElementMemory extends Colored implements OsmElement {
    private final long id;
    private final BoundingBox boundingBox;
    private final double area;

    public OsmElementMemory(long id, BoundingBox boundingBox, double area) {
        this.id = id;
        this.boundingBox = boundingBox;
        this.area = area;
    }

    public double getArea() { return area; }

    public BoundingBox getBoundingBox() { return boundingBox; }

    public double distance(OsmElementMemory other) {
        return this.boundingBox.distanceToBoundingBox(other.boundingBox);
    }

    @Override
    public long getId() { return id; }
}
