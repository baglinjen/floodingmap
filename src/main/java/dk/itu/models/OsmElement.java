package dk.itu.models;

import java.io.Serializable;

public abstract class OsmElement implements Serializable {
    public abstract long getId();
    abstract float getMinX();
    abstract float getMaxX();
    abstract float getMinY();
    abstract float getMaxY();
    public boolean equals(OsmElement obj) {
        return this.getId() == obj.getId();
    }
}