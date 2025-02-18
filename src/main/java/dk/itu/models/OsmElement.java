package dk.itu.models;

import java.io.Serializable;

public abstract class OsmElement implements Serializable {
    public abstract long getId();
    abstract double getMinLon();
    abstract double getMaxLon();
    abstract double getMinLat();
    abstract double getMaxLat();
    public boolean equals(OsmElement obj) {
        return this.getId() == obj.getId();
    }
    public abstract double getArea();
}