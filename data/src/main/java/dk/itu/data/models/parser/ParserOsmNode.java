package dk.itu.data.models.parser;

import java.awt.*;
import java.awt.geom.Path2D;

public class ParserOsmNode extends ParserOsmElement {
    private final double lat, lon;
    private boolean isRouting;

    public ParserOsmNode(long id, double lat, double lon) {
        super(id);
        this.lat = lat;
        this.lon = lon;
    }

    public boolean isRouting(){return isRouting;}
    public void setRouting(){isRouting = true;}

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
    public Path2D.Double getShape() {
        return null;
    }

    @Override
    public double[] getBounds() {
        return new double[]{lon, lat, lon, lat};
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) { /* Nodes are not drawn for now */ }
}
