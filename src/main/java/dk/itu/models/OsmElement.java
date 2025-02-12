package dk.itu.models;

import java.io.Serializable;

public abstract class OsmElement implements Serializable {
    public abstract long getId();
    abstract float getMinLon();
    abstract float getMaxLon();
    abstract float getMinLat();
    abstract float getMaxLat();
    public boolean equals(OsmElement obj) {
        return this.getId() == obj.getId();
    }
}