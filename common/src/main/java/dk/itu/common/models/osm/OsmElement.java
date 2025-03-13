package dk.itu.common.models.osm;

import dk.itu.common.models.Drawable;
import dk.itu.common.models.Geographical2D;

import java.awt.*;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;

public abstract class OsmElement extends Drawable implements Geographical2D {
    public long id;
    protected Geometry geometry;

    public OsmElement(long id, Geometry geometry) {
        this.id = id;
        this.geometry = geometry;
    }

    public long getId() {
        return id;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public abstract double getArea();

    public abstract boolean shouldBeDrawn();
}

