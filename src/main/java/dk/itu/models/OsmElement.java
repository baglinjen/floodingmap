package dk.itu.models;

import java.io.Serializable;

public interface OsmElement extends Serializable {
    long getId();
    float getMinX();
    float getMaxX();
    float getMinY();
    float getMaxY();
}