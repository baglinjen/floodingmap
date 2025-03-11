package dk.itu.common.models.osm;

import java.awt.*;
import java.awt.geom.Point2D;

public class OsmNode extends OsmElement {
    private final double lat, lon;
    public OsmNode(long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public Shape getShape() {
        return null;
    }

    @Override
    public double[] getBounds() {
        return new double[]{lat, lon, lat, lon};
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
