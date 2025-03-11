package dk.itu.common.models.osm;

import dk.itu.common.models.Drawable;
import dk.itu.common.models.Geographical2D;

import java.awt.*;

public abstract class OsmElement extends Drawable implements Geographical2D {
    public long id;

    public OsmElement(long id) {
        this.id = id;
    }

    public abstract double getArea();
    public abstract Shape getShape();
}
